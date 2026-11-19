import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
import { createLocalSupabase } from './local-supabase.mjs';
const { PGlite } = await import(process.env.PGLITE_MODULE ? pathToFileURL(process.env.PGLITE_MODULE).href : '@electric-sql/pglite');
const db = await PGlite.create();
const alice = '10000000-0000-0000-0000-000000000001';
const bob = '10000000-0000-0000-0000-000000000002';
const charlie = '10000000-0000-0000-0000-000000000003';
let passed = 0;
const query = (sql, args) => db.query(sql, args);
const admin = () => db.exec('reset role');
const test = async (name, run) => { await run(); console.log(`OK amis ${++passed} — ${name}`); };
const denied = (sql, args, code = '42501') => assert.rejects(() => query(sql, args), error => error.code === code);
async function login(uid, provider = 'google', role = 'authenticated') {
    await admin();
    await query("select set_config('request.jwt.claims',$1,false)", [JSON.stringify({ sub: uid, app_metadata: { provider } })]);
    await db.exec('set role ' + role);
}
const snapshot = async () => (await query('select public.get_friend_snapshot() as data')).rows[0].data;
const action = async (kind, target = null, code = null, id = null) =>
    (await query('select public.friend_action($1,$2,$3,$4) as data', [kind, target, code, id])).rows[0].data;
const profile = async uid => (await query('select public.get_social_profile($1) as data', [uid])).rows[0].data;
try {
    await createLocalSupabase(db);
    await query('insert into auth.users values($1,$4::jsonb),($2,$5::jsonb),($3,$6::jsonb)',
        [alice, bob, charlie, JSON.stringify({ full_name: 'Alice\n', email: 'private@example.test' }),
            JSON.stringify({ full_name: 'Bob' }), JSON.stringify({ name: 'Charlie' })]);
    const migrations = await Promise.all(['202610150001_levels_and_rankings.sql', '202611180001_courses_missions_and_skins.sql',
        '202611180002_friend_requests_and_profiles.sql'].map(name => readFile(new URL('../migrations/' + name, import.meta.url), 'utf8')));
    for (const migration of migrations) await db.exec(migration);
    await test('profils existants repris, codes uniques et noms nettoyés', async () => {
        const rows = (await query('select * from public.player_profiles')).rows;
        assert.equal(rows.length, 3); assert.equal(new Set(rows.map(r => r.friend_code)).size, 3);
        assert.ok(rows.every(r => /^[A-F0-9]{12}$/.test(r.friend_code)));
        assert.equal(rows.find(r => r.uid === alice).display_name, 'Alice');
    });
    await test('les nouveaux comptes reçoivent aussi un code sans réinitialiser les précédents', async () => {
        const code = (await query('select friend_code from public.player_profiles where uid=$1', [alice])).rows[0].friend_code;
        await query("update auth.users set raw_user_meta_data='{}' where id=$1", [charlie]);
        assert.equal((await query('select display_name from public.player_profiles where uid=$1', [charlie])).rows[0].display_name, 'Joueur');
        await db.exec(migrations[2]);
        assert.equal((await query('select friend_code from public.player_profiles where uid=$1', [alice])).rows[0].friend_code, code);
    });
    await test('RPC interdites à anon, aux UID absents et aux autres fournisseurs', async () => {
        for (const [uid, provider, role] of [[alice, 'google', 'anon'], [null, 'google', 'authenticated'],
            [alice, 'email', 'authenticated'], ['10000000-0000-0000-0000-999999999999', 'google', 'authenticated']]) {
            await login(uid, provider, role);
            await denied('select public.get_friend_snapshot()');
            await denied("select public.friend_action('send',$1)", [bob]);
            await denied('select public.get_social_profile($1)', [bob]);
        }
    });
    await test('tables et helpers inaccessibles directement aux clients', async () => {
        await login(alice);
        for (const table of ['player_profiles', 'friend_relations']) {
            await denied('select * from public.' + table);
            await denied('delete from public.' + table);
        }
        await denied('select public.require_social_user()');
        await denied('select public.ensure_player_profile($1)', [bob]);
    });
    await test('snapshot avec provisionnement : transaction lecture seule refusée, lecture-écriture autorisée', async () => {
        await login(alice);
        await db.exec('begin read only');
        try {
            await denied('select public.get_friend_snapshot()', [], '25006');
        } finally { await db.exec('rollback'); }
        await db.exec('begin read write');
        try {
            assert.equal((await snapshot()).viewer_uid, alice);
        } finally { await db.exec('rollback'); }
    });
    await test('ajout de soi-même et cibles ambiguës ou inconnues refusés', async () => {
        await denied("select public.friend_action('send',$1)", [alice], '22023');
        await denied("select public.friend_action('send',null,null,null)", [], '22023');
        await denied("select public.friend_action('send',$1,'ABCDEF123456')", [bob], '22023');
        await denied("select public.friend_action('send',null,'invalid')", [], '22023');
        await denied("select public.friend_action('send',null,'FFFFFFFFFFFF')", [], '22023');
        await denied("select public.friend_action('arbitrary',$1)", [bob], '22023');
    });
    let relationId;
    await test('demande depuis un UID : propriétaire et destinataire dérivés côté serveur', async () => {
        const data = await action('send', bob);
        assert.equal(data.viewer_uid, alice); assert.equal(data.relations.length, 1);
        assert.equal(data.relations[0].uid, bob); assert.equal(data.relations[0].status, 'outgoing');
        relationId = data.relations[0].id;
        assert.equal(Number(data.revision), 1);
        await login(bob); assert.equal((await snapshot()).relations[0].status, 'incoming');
    });
    await test('doublons et demandes croisées ne créent ni doublon ni acceptation implicite', async () => {
        assert.equal((await action('send', alice)).relations[0].id, relationId);
        await login(alice);
        assert.equal((await action('send', bob)).relations[0].id, relationId);
        assert.equal((await snapshot()).relations[0].status, 'outgoing');
        await admin(); assert.equal((await query('select count(*) as n from public.friend_relations')).rows[0].n, 1);
    });
    await test('un tiers ne peut accepter, refuser, annuler ou supprimer une relation', async () => {
        await login(charlie);
        for (const kind of ['accept', 'decline', 'cancel', 'remove']) {
            await denied('select public.friend_action($1,null,null,$2)', [kind, relationId]);
        }
        assert.deepEqual((await snapshot()).relations, []);
    });
    await test('seul le destinataire accepte ou refuse ; seul l’émetteur annule', async () => {
        await login(alice);
        for (const kind of ['accept', 'decline', 'remove']) await denied('select public.friend_action($1,null,null,$2)', [kind, relationId]);
        await login(bob);
        await denied("select public.friend_action('cancel',null,null,$1)", [relationId]);
        const data = await action('accept', null, null, relationId);
        assert.equal(data.relations[0].status, 'friend');
        assert.equal((await action('accept', null, null, relationId)).relations[0].status, 'friend');
        await login(alice); assert.equal((await snapshot()).relations[0].status, 'friend');
    });
    await test('une amitié peut être retirée des deux côtés sans être recréée par une réponse retardée', async () => {
        assert.deepEqual((await action('remove', null, null, relationId)).relations, []);
        assert.deepEqual((await action('remove', null, null, relationId)).relations, []);
        const replacement = (await action('send', bob)).relations[0].id;
        assert.notEqual(replacement, relationId);
        await action('cancel', null, null, relationId);
        assert.equal((await snapshot()).relations[0].id, replacement);
        relationId = replacement;
    });
    await test('refus puis annulation suppriment bien les demandes', async () => {
        await login(bob); assert.deepEqual((await action('decline', null, null, relationId)).relations, []);
        await login(alice); relationId = (await action('send', bob)).relations[0].id;
        assert.deepEqual((await action('cancel', null, null, relationId)).relations, []);
    });
    await test('code ami insensible à la casse, espaces et séparateurs', async () => {
        await login(bob); const code = (await snapshot()).friend_code;
        await login(alice);
        const spaced = code.match(/.{4}/g).join(' - ').toLowerCase();
        const data = await action('send', null, spaced);
        assert.equal(data.relations[0].uid, bob); relationId = data.relations[0].id;
        await denied('select public.friend_action($1,null,$2)', ['send', (await snapshot()).friend_code], '22023');
    });
    await test('la contrainte de paire interdit aussi les doublons et relations avec soi-même', async () => {
        await admin();
        await assert.rejects(() => query('insert into public.friend_relations(low_uid,high_uid,requester_uid) values($1,$2,$1)', [alice, bob]), e => e.code === '23505');
        await assert.rejects(() => query('insert into public.friend_relations(low_uid,high_uid,requester_uid) values($1,$1,$1)', [alice]), e => e.code === '23514');
        assert.ok(migrations[2].includes('order by uid for update'));
    });
    await test('les événements serveur ne contiennent que des invalidations sur deux topics privés', async () => {
        const rows = (await query('select * from realtime.messages')).rows;
        assert.ok(rows.length >= 2);
        assert.ok(rows.every(r => r.private && r.event === 'friends_changed' && JSON.stringify(r.payload) === '{"version":1}'));
        assert.deepEqual(new Set(rows.map(r => r.topic)), new Set(['friends:' + alice, 'friends:' + bob]));
    });
    await test('canal personnel autorisé, canal d’un autre compte et fournisseur non Google refusés', async () => {
        await login(alice); await query("select set_config('realtime.topic',$1,false)", ['friends:' + alice]);
        assert.ok((await query('select * from realtime.messages')).rows.length > 0);
        await query("select set_config('realtime.topic',$1,false)", ['friends:' + bob]);
        assert.equal((await query('select * from realtime.messages')).rows.length, 0);
        await login(alice, 'email'); await query("select set_config('realtime.topic',$1,false)", ['friends:' + alice]);
        assert.equal((await query('select * from realtime.messages')).rows.length, 0);
    });
    await test('les anciennes policies permissives ne permettent ni écoute étrangère ni fausse alerte', async () => {
        await admin();
        await db.exec('create policy legacy_receive on realtime.messages for select to public using(true); create policy legacy_send on realtime.messages for insert to public with check(true)');
        await login(alice); await query("select set_config('realtime.topic',$1,false)", ['friends:' + bob]);
        assert.equal((await query('select * from realtime.messages')).rows.length, 0);
        await denied("insert into realtime.messages values($1,'broadcast','{}','friends_changed',true)", ['friends:' + alice]);
        await admin(); await db.exec('grant select, insert on realtime.messages to anon');
        await login(null, 'google', 'anon');
        await query("select set_config('realtime.topic',$1,false)", ['friends:' + alice]);
        assert.equal((await query('select * from realtime.messages')).rows.length, 0);
        await denied("insert into realtime.messages values($1,'broadcast','{}','friends_changed',true)", ['friends:' + alice]);
        await admin(); await db.exec('revoke select, insert on realtime.messages from anon; drop policy legacy_receive on realtime.messages; drop policy legacy_send on realtime.messages');
    });
    await test('profils : pas d’email, de jeton ni de code ami d’un autre joueur', async () => {
        await login(alice);
        const data = await profile(bob);
        assert.equal(data.uid, bob); assert.equal(data.friend_code, null);
        assert.deepEqual(Object.keys(data).sort(), ['display_name', 'friend_code', 'records', 'skin_id', 'uid']);
        assert.equal((await profile(alice)).friend_code, (await snapshot()).friend_code);
        await denied('select public.get_social_profile($1)', [charlie], '22023');
    });
    await test('un profil du classement expose uniquement ses records actifs', async () => {
        await login(charlie); await query("select public.submit_run('prairie-1-v2',2,42000,1)");
        await login(alice);
        const data = await profile(charlie);
        assert.deepEqual(data.records, [{ level_id: 'prairie-1-v2', time_ms: 42000 }]);
    });
    await test('limite de nouveaux envois par jour, sans compter les doublons', async () => {
        await admin(); await query("update public.player_profiles set request_day=(now() at time zone 'UTC')::date, requests_today=30 where uid=$1", [alice]);
        await login(alice);
        assert.equal((await action('send', bob)).relations[0].id, relationId);
        await denied("select public.friend_action('send',$1)", [charlie], '22023');
        await admin(); await query("update public.player_profiles set request_day='2000-01-01' where uid=$1", [alice]);
        await login(alice); assert.equal((await action('send', charlie)).relations.length, 2);
    });
    await test('les plafonds de demandes et d’amis sont contrôlés pour les deux participants', async () => {
        await admin();
        const limited = '20000000-0000-0000-0000-000000000001';
        await db.exec("insert into auth.users select ('20000000-0000-0000-0000-'||lpad(i::text,12,'0'))::uuid,'{}'::jsonb from generate_series(1,201) i");
        assert.equal((await query("select count(*) as n from public.player_profiles where uid::text like '20000000-%'")).rows[0].n, 201);
        await query("insert into public.friend_relations(low_uid,high_uid,requester_uid) select least($1::uuid,uid),greatest($1::uuid,uid),uid from public.player_profiles where uid::text like '20000000-%' and uid<>$1 order by uid limit 100", [limited]);
        await login(alice); await denied("select public.friend_action('send',$1)", [limited], '22023');
        await login(limited); await denied("select public.friend_action('send',$1)", [alice], '22023');
        await admin(); await query('delete from public.friend_relations where $1 in (low_uid,high_uid)', [limited]);
        await login(alice); const pending = (await action('send', limited)).relations.find(r => r.uid === limited).id;
        await admin();
        await query("insert into public.friend_relations(low_uid,high_uid,requester_uid,status) select least($1::uuid,uid),greatest($1::uuid,uid),uid,'accepted' from public.player_profiles where uid::text like '20000000-%' and uid<>$1", [limited]);
        await login(charlie); await denied("select public.friend_action('send',$1)", [limited], '22023');
        await login(limited); await denied("select public.friend_action('send',$1)", [charlie], '22023');
        await denied("select public.friend_action('accept',null,null,$1)", [pending], '22023');
        await admin(); await db.exec("delete from auth.users where id::text like '20000000-%'");
    });
    await test('RLS défensive conserve l’isolation si SELECT est accordé par erreur', async () => {
        await admin(); await db.exec('grant select on public.player_profiles,public.friend_relations to authenticated');
        await login(bob);
        assert.equal((await query('select * from public.player_profiles')).rows.length, 1);
        assert.equal((await query('select * from public.friend_relations')).rows.length, 1);
        await login(bob, 'email'); assert.equal((await query('select * from public.friend_relations')).rows.length, 0);
        await admin(); await db.exec('revoke select on public.player_profiles,public.friend_relations from authenticated');
    });
    await test('rejouer les migrations conserve relations, codes et records', async () => {
        await login(alice); const before = await snapshot();
        await admin(); for (const migration of migrations) await db.exec(migration);
        await login(alice); const after = await snapshot();
        assert.deepEqual(after, before); assert.equal((await profile(charlie)).records.length, 1);
    });
    await test('suppression d’un compte : cascades limitées à ses relations et son profil', async () => {
        await admin(); await query('delete from auth.users where id=$1', [bob]);
        assert.equal((await query('select count(*) as n from public.player_profiles')).rows[0].n, 2);
        await login(alice); assert.equal((await snapshot()).relations.length, 1);
        assert.equal((await snapshot()).relations[0].uid, charlie);
    });
    console.log(`${passed} tests sociaux SQL réussis sur Postgres local en mémoire.`);
} finally { await db.close(); }
