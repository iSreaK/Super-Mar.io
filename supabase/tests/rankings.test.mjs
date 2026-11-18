import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
const { PGlite } = await import(process.env.PGLITE_MODULE ? pathToFileURL(process.env.PGLITE_MODULE).href : '@electric-sql/pglite');
const database = await PGlite.create();
let passed = 0;
const alice = '00000000-0000-0000-0000-000000000001';
const bob = '00000000-0000-0000-0000-000000000002';
const meadow = 'prairie-1-v2';
const night = 'night-3-v1';
const test = async (name, run) => { await run(); passed++; console.log('OK ' + passed + ' — ' + name); };
const query = (sql, params) => database.query(sql, params);
const admin = () => database.exec('reset role');
async function login(uid, provider = 'google', role = 'authenticated') {
    await admin();
    await query("select set_config('request.jwt.claims', $1, false)", [JSON.stringify({ sub: uid, app_metadata: { provider } })]);
    await database.exec('set role ' + role);
}
const denied = (sql, params, code = '42501') => assert.rejects(() => query(sql, params), error => error.code === code);
const submit = (level, version, time, coins = 2) => query('select public.submit_run($1, $2, $3, $4)', [level, version, time, coins]);
try {
    // Auth et rôles minimaux locaux : aucun appel à un compte ou projet Supabase réel.
    await database.exec(
        "create role anon; create role authenticated; create schema auth;" +
        "create table auth.users (id uuid primary key, raw_user_meta_data jsonb);" +
        "create function auth.jwt() returns jsonb language sql stable as $$ select coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb $$;" +
        "create function auth.uid() returns uuid language sql stable as $$ select (auth.jwt() ->> 'sub')::uuid $$;" +
        "grant usage on schema auth to anon, authenticated;"
    );
    await query('insert into auth.users values ($1, $3::jsonb), ($2, $4::jsonb)', [alice, bob,
        JSON.stringify({ full_name: '  Alice\n' + 'x'.repeat(60) + '  ', email: 'private@example.test' }), JSON.stringify({ full_name: 'Bob' })]);
    const migration = await readFile(new URL('../migrations/202610150001_levels_and_rankings.sql', import.meta.url), 'utf8');
    await test('migration atomique et rejouable', async () => { await database.exec(migration); await database.exec(migration); });
    await test('trois niveaux et RLS activée', async () => {
        assert.equal((await query('select * from public.game_levels')).rows.length, 3);
        assert.ok((await query("select relrowsecurity from pg_class where oid in ('public.game_levels'::regclass, 'public.best_runs'::regclass)")).rows.every(row => row.relrowsecurity));
    });
    await test('anon ne peut appeler aucune RPC', async () => {
        await login(null, 'google', 'anon');
        await denied('select * from public.get_leaderboard($1)', [meadow]);
        await denied('select public.submit_run($1, 2, 42000, 1)', [meadow]);
    });
    await test('une session sans UID est refusée', async () => {
        await login(null); await denied('select * from public.get_leaderboard($1)', [meadow]);
        await denied('select public.submit_run($1, 2, 42000, 1)', [meadow]);
    });
    await test('un fournisseur autre que Google est refusé', async () => {
        await login(alice, 'email'); await denied('select * from public.get_leaderboard($1)', [meadow]);
        await denied('select public.submit_run($1, 2, 42000, 1)', [meadow]);
    });
    await test('aucune permission directe sur les tables', async () => {
        await login(alice);
        await denied('select * from public.best_runs');
        await denied("insert into public.best_runs values ('prairie-1-v2', $1, 42000, 1, 'Fake', now())", [bob]);
        await denied('update public.best_runs set time_ms = 10000');
        await denied('delete from public.best_runs');
    });
    await test('le serveur déduit UID et nom, nettoie et borne le pseudonyme', async () => {
        await login(alice); await submit(meadow, 2, 42000);
        const rows = (await query('select * from public.get_leaderboard($1)', [meadow])).rows;
        assert.equal(rows[0].uid, alice); assert.equal(rows[0].display_name.length, 40);
        assert.ok(!rows[0].display_name.includes('\n')); assert.ok(!Object.hasOwn(rows[0], 'email'));
    });
    await test('un temps égal ou moins bon ne remplace pas le record', async () => {
        await submit(meadow, 2, 45000, 7); await submit(meadow, 2, 42000, 8);
        const row = (await query('select * from public.get_leaderboard($1)', [meadow])).rows[0];
        assert.equal(Number(row.time_ms), 42000); assert.equal(row.coins, 2);
    });
    await test('un meilleur temps remplace atomiquement le précédent', async () => {
        await submit(meadow, 2, 40000, 5);
        const row = (await query('select * from public.get_leaderboard($1)', [meadow])).rows[0];
        assert.equal(Number(row.time_ms), 40000); assert.equal(row.coins, 5);
    });
    await test('les comptes ne peuvent écraser les records des autres', async () => {
        await login(bob); await submit(meadow, 2, 43000);
        const rows = (await query('select * from public.get_leaderboard($1)', [meadow])).rows;
        assert.equal(rows.length, 2); assert.equal(Number(rows.find(row => row.uid === alice).time_ms), 40000);
    });
    await test('les niveaux ont des records séparés', async () => {
        await login(alice); await submit(night, 1, 45000);
        assert.equal(Number((await query('select * from public.get_leaderboard($1)', [night])).rows[0].time_ms), 45000);
        assert.equal(Number((await query('select * from public.get_leaderboard($1)', [meadow])).rows[0].time_ms), 40000);
    });
    await test('temps, pièces et valeurs nulles sont validés', async () => {
        for (const [time, coins] of [[9999, 1], [600001, 1], [42000, -1], [42000, 101], [null, 1], [42000, null]]) {
            await denied('select public.submit_run($1, 2, $2, $3)', [meadow, time, coins], '22023');
        }
    });
    await test('versions inconnues, parcours historique et niveaux inactifs sont refusés', async () => {
        await denied('select public.submit_run($1, 1, 42000, 1)', [meadow], '22023');
        await denied('select public.submit_run($1, 1, 42000, 1)', ['prairie-1-v1'], '22023');
        await admin(); await query('update public.game_levels set active = false where id = $1', [night]); await login(alice);
        await denied('select * from public.get_leaderboard($1)', [night], '22023');
        await denied('select public.submit_run($1, 1, 42000, 1)', [night], '22023');
        await admin(); await query('update public.game_levels set active = true where id = $1', [night]); await login(alice);
    });
    await test('un UID absent de Auth ne peut soumettre', async () => {
        await login('00000000-0000-0000-0000-999999999999');
        await denied('select public.submit_run($1, 2, 42000, 1)', [meadow]); await login(alice);
    });
    await test('classement borné à 50 et égalités déterministes', async () => {
        await admin();
        await database.exec("insert into auth.users select ('00000000-0000-0000-0000-' || lpad(i::text,12,'0'))::uuid, '{}'::jsonb from generate_series(3,65) i;");
        await query("insert into public.best_runs select $1, id, 41000, 0, 'Player', now() from auth.users where id not in ($2::uuid, $3::uuid)", [meadow, alice, bob]);
        await login(alice);
        const rows = (await query('select * from public.get_leaderboard($1)', [meadow])).rows;
        assert.equal(rows.length, 50); assert.equal(rows[0].uid, alice);
        assert.equal(rows[1].uid, '00000000-0000-0000-0000-000000000003');
        assert.equal(rows[2].uid, '00000000-0000-0000-0000-000000000004');
        assert.equal((await query('select * from public.get_leaderboard($1)', [night])).rows.length, 1);
    });
    await test('la RLS filtre encore si SELECT était accordé par erreur', async () => {
        await admin(); await database.exec('grant select on public.best_runs to authenticated');
        await login(alice, 'email'); assert.equal((await query('select * from public.best_runs')).rows.length, 0);
        await login(alice); assert.ok((await query('select * from public.best_runs')).rows.length > 0);
        await admin(); await database.exec('revoke select on public.best_runs from authenticated');
    });
    await test('rejouer la migration conserve les records', async () => {
        await admin(); await database.exec(migration); await login(alice);
        assert.equal(Number((await query('select * from public.get_leaderboard($1)', [night])).rows[0].time_ms), 45000);
    });
    await test('suppression de compte : cascade limitée à ses records', async () => {
        await admin(); await query('delete from auth.users where id = $1', [alice]);
        assert.equal((await query('select * from public.best_runs where user_id = $1', [alice])).rows.length, 0);
        assert.equal((await query('select * from public.best_runs where user_id = $1', [bob])).rows.length, 1);
    });
    console.log(passed + ' tests SQL réussis sur Postgres local en mémoire.');
} finally { await database.close(); }
