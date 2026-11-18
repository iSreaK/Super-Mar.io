-- Extension additive. Appliquer après les deux migrations précédentes.
-- Realtime Broadcast privé ; aucune publication des tables métier ou de données Auth.
begin;

do $$ begin
    if to_regclass('realtime.messages') is null
        or to_regprocedure('realtime.send(jsonb,text,text,boolean)') is null then
        raise exception 'Supabase Realtime doit être disponible avant cette migration';
    end if;
end $$;

create table if not exists public.player_profiles (
    uid uuid primary key references auth.users(id) on delete cascade,
    friend_code text not null unique check (friend_code ~ '^[A-F0-9]{12}$'),
    display_name text not null check (char_length(display_name) between 1 and 40),
    social_revision bigint not null default 0 check (social_revision >= 0),
    request_day date,
    requests_today integer not null default 0 check (requests_today >= 0)
);
create table if not exists public.friend_relations (
    id uuid not null unique default gen_random_uuid(),
    low_uid uuid not null references public.player_profiles(uid) on delete cascade,
    high_uid uuid not null references public.player_profiles(uid) on delete cascade,
    requester_uid uuid not null references public.player_profiles(uid) on delete cascade,
    status text not null default 'pending' check (status in ('pending', 'accepted')),
    created_at timestamptz not null default now(),
    primary key (low_uid, high_uid),
    check (low_uid < high_uid),
    check (requester_uid in (low_uid, high_uid))
);
create index if not exists friend_relations_high_uid_idx on public.friend_relations(high_uid);
alter table public.player_profiles enable row level security;
alter table public.friend_relations enable row level security;
revoke all on public.player_profiles, public.friend_relations from public, anon, authenticated;
drop policy if exists own_social_profile on public.player_profiles;
create policy own_social_profile on public.player_profiles for select to authenticated
using (uid = (select auth.uid()) and (select auth.jwt()) -> 'app_metadata' ->> 'provider' = 'google');
drop policy if exists own_friend_relations on public.friend_relations;
create policy own_friend_relations on public.friend_relations for select to authenticated
using ((select auth.uid()) in (low_uid, high_uid)
    and (select auth.jwt()) -> 'app_metadata' ->> 'provider' = 'google');

-- Helpers privés : aucun UID appelant n'est accepté comme autorité client.
create or replace function public.require_social_user()
returns uuid language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid();
begin
    if actor is null or coalesce(auth.jwt() -> 'app_metadata' ->> 'provider', '') <> 'google'
        or not exists (select 1 from auth.users where id = actor) then
        raise exception 'Google authentication required' using errcode = '42501';
    end if;
    return actor;
end $$;

create or replace function public.ensure_player_profile(p_uid uuid)
returns void language plpgsql security definer set search_path = '' as $$
declare player_name text; attempts integer := 0;
begin
    select left(trim(regexp_replace(coalesce(raw_user_meta_data ->> 'full_name',
        raw_user_meta_data ->> 'name', ''), '[[:cntrl:]]', '', 'g')), 40)
        into player_name from auth.users where id = p_uid;
    if not found then raise exception 'Unknown user' using errcode = '42501'; end if;
    player_name := coalesce(nullif(player_name, ''), 'Joueur');
    loop
        begin
            insert into public.player_profiles(uid, friend_code, display_name)
            values (p_uid, upper(left(replace(gen_random_uuid()::text, '-', ''), 12)), player_name)
            on conflict (uid) do update set display_name = excluded.display_name
                where public.player_profiles.display_name is distinct from excluded.display_name;
            return;
        exception when unique_violation then
            attempts := attempts + 1;
            if attempts >= 8 then raise; end if;
        end;
    end loop;
end $$;

create or replace function public.sync_auth_player_profile()
returns trigger language plpgsql security definer set search_path = '' as $$
begin perform public.ensure_player_profile(new.id); return new; end $$;
drop trigger if exists mariosprint_player_profile on auth.users;
create trigger mariosprint_player_profile after insert or update of raw_user_meta_data on auth.users
for each row execute function public.sync_auth_player_profile();
do $$ declare player record; begin
    for player in select id from auth.users order by id loop
        perform public.ensure_player_profile(player.id);
    end loop;
end $$;

create or replace function public.broadcast_friend_change()
returns trigger language plpgsql security definer set search_path = '' as $$
declare first_uid uuid; second_uid uuid;
begin
    if tg_op = 'DELETE' then first_uid := old.low_uid; second_uid := old.high_uid;
    else first_uid := new.low_uid; second_uid := new.high_uid; end if;
    update public.player_profiles set social_revision = social_revision + 1
        where uid in (first_uid, second_uid);
    -- Invalidation uniquement : les données sont toujours relues via une RPC autorisée.
    perform realtime.send(jsonb_build_object('version', 1), 'friends_changed', 'friends:' || first_uid::text, true);
    perform realtime.send(jsonb_build_object('version', 1), 'friends_changed', 'friends:' || second_uid::text, true);
    return null;
end $$;
drop trigger if exists mariosprint_friend_change on public.friend_relations;
create trigger mariosprint_friend_change after insert or update or delete on public.friend_relations
for each row execute function public.broadcast_friend_change();

-- Ne pas ALTER realtime.messages : Supabase en gère la propriété et la RLS.
-- Les politiques restrictives protègent aussi ce namespace d'une ancienne policy trop large.
drop policy if exists mariosprint_friends_receive on realtime.messages;
create policy mariosprint_friends_receive on realtime.messages for select to authenticated
using (extension = 'broadcast' and (select realtime.topic()) = 'friends:' || (select auth.uid())::text
    and (select auth.jwt()) -> 'app_metadata' ->> 'provider' = 'google');
drop policy if exists mariosprint_friends_scope on realtime.messages;
create policy mariosprint_friends_scope on realtime.messages as restrictive for select to public
using ((select realtime.topic()) not like 'friends:%'
    or (extension = 'broadcast' and (select realtime.topic()) = 'friends:' || (select auth.uid())::text
        and (select auth.jwt()) -> 'app_metadata' ->> 'provider' = 'google'));
drop policy if exists mariosprint_friends_no_client_send on realtime.messages;
create policy mariosprint_friends_no_client_send on realtime.messages as restrictive for insert to public
with check (topic not like 'friends:%');

create or replace function public.get_friend_snapshot()
returns jsonb language plpgsql security definer set search_path = '' as $$
declare actor uuid := public.require_social_user(); result jsonb;
begin
    perform public.ensure_player_profile(actor);
    select jsonb_build_object('viewer_uid', actor, 'revision', p.social_revision,
        'friend_code', p.friend_code, 'relations', coalesce((
            select jsonb_agg(jsonb_build_object('id', r.id, 'uid', peer.uid,
                'display_name', peer.display_name, 'skin_id', coalesce(a.skin_id, 'luigi'),
                'status', case when r.status = 'accepted' then 'friend'
                    when r.requester_uid = actor then 'outgoing' else 'incoming' end)
                order by r.created_at, r.id)
            from public.friend_relations r
            join public.player_profiles peer on peer.uid = case when r.low_uid = actor then r.high_uid else r.low_uid end
            left join public.player_appearance a on a.user_id = peer.uid
            where actor in (r.low_uid, r.high_uid)
        ), '[]'::jsonb)) into result from public.player_profiles p where p.uid = actor;
    return result;
end $$;

create or replace function public.friend_action(p_action text, p_target_uid uuid default null,
    p_code text default null, p_relation_id uuid default null)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    actor uuid := public.require_social_user(); target uuid; relation public.friend_relations%rowtype;
    code text; pending_count integer; friend_count integer; today date := (now() at time zone 'UTC')::date;
begin
    if p_action is null or p_action not in ('send', 'accept', 'decline', 'cancel', 'remove') then
        raise exception 'friend_invalid_action' using errcode = '22023';
    end if;
    if p_action = 'send' then
        if (p_target_uid is null) = (p_code is null) or p_relation_id is not null then
            raise exception 'friend_invalid_target' using errcode = '22023';
        end if;
        if p_code is not null then
            code := upper(regexp_replace(trim(p_code), '[-[:space:]]', '', 'g'));
            if code !~ '^[A-F0-9]{12}$' then
                raise exception 'friend_invalid_code' using errcode = '22023';
            end if;
            select uid into target from public.player_profiles where friend_code = code;
        else target := p_target_uid; end if;
        if target is null or not exists (select 1 from public.player_profiles where uid = target) then
            raise exception 'friend_not_found' using errcode = '22023';
        end if;
        if actor = target then raise exception 'friend_self' using errcode = '22023'; end if;
    else
        if p_relation_id is null or p_target_uid is not null or p_code is not null then
            raise exception 'friend_invalid_target' using errcode = '22023';
        end if;
        select * into relation from public.friend_relations where id = p_relation_id;
        if not found then return public.get_friend_snapshot(); end if;
        if actor not in (relation.low_uid, relation.high_uid) then
            raise exception 'friend_forbidden' using errcode = '42501';
        end if;
        target := case when relation.low_uid = actor then relation.high_uid else relation.low_uid end;
    end if;
    -- Même ordre pour toutes les paires : sérialise aussi les quotas par utilisateur.
    perform uid from public.player_profiles where uid in (actor, target) order by uid for update;
    select * into relation from public.friend_relations
        where low_uid = least(actor, target) and high_uid = greatest(actor, target) for update;
    if p_action = 'send' then
        -- Une demande croisée reste une demande reçue, jamais une acceptation implicite.
        if found then return public.get_friend_snapshot(); end if;
        select count(*) filter (where status = 'pending'), count(*) filter (where status = 'accepted')
            into pending_count, friend_count from public.friend_relations where actor in (low_uid, high_uid);
        if pending_count >= 100 or friend_count >= 200 then
            raise exception 'friend_limit' using errcode = '22023';
        end if;
        select count(*) filter (where status = 'pending'), count(*) filter (where status = 'accepted')
            into pending_count, friend_count from public.friend_relations where target in (low_uid, high_uid);
        if pending_count >= 100 or friend_count >= 200 then
            raise exception 'friend_limit' using errcode = '22023';
        end if;
        if exists (select 1 from public.player_profiles where uid = actor and request_day = today and requests_today >= 30) then
            raise exception 'friend_rate_limit' using errcode = '22023';
        end if;
        update public.player_profiles set requests_today = case when request_day = today then requests_today + 1 else 1 end,
            request_day = today where uid = actor;
        insert into public.friend_relations(low_uid, high_uid, requester_uid)
            values (least(actor, target), greatest(actor, target), actor);
    else
        -- Une commande retardée ne touche jamais une nouvelle relation créée entre les mêmes joueurs.
        if not found or relation.id <> p_relation_id then return public.get_friend_snapshot(); end if;
        if p_action = 'accept' then
            if relation.requester_uid = actor then raise exception 'friend_forbidden' using errcode = '42501'; end if;
            if relation.status = 'pending' then
                if (select count(*) from public.friend_relations where status = 'accepted' and actor in (low_uid, high_uid)) >= 200
                    or (select count(*) from public.friend_relations where status = 'accepted' and target in (low_uid, high_uid)) >= 200 then
                    raise exception 'friend_limit' using errcode = '22023';
                end if;
                update public.friend_relations set status = 'accepted' where id = relation.id;
            end if;
        elsif p_action = 'decline' and relation.status = 'pending' and relation.requester_uid <> actor
            or p_action = 'cancel' and relation.status = 'pending' and relation.requester_uid = actor
            or p_action = 'remove' and relation.status = 'accepted' then
            delete from public.friend_relations where id = relation.id;
        else raise exception 'friend_forbidden' using errcode = '42501'; end if;
    end if;
    return public.get_friend_snapshot();
end $$;

create or replace function public.get_social_profile(p_uid uuid)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare actor uuid := public.require_social_user(); result jsonb;
begin
    if p_uid is null or not exists (select 1 from public.player_profiles where uid = p_uid)
        or (p_uid <> actor and not exists (select 1 from public.friend_relations where low_uid = least(actor, p_uid)
                and high_uid = greatest(actor, p_uid))
            and not exists (select 1 from public.best_runs r join public.game_levels l on l.id = r.level_id
                where r.user_id = p_uid and l.active)) then
        raise exception 'friend_not_found' using errcode = '22023';
    end if;
    select jsonb_build_object('uid', p.uid, 'display_name', p.display_name,
        'skin_id', coalesce(a.skin_id, 'luigi'), 'friend_code', case when p.uid = actor then p.friend_code else null end,
        'records', coalesce((select jsonb_agg(jsonb_build_object('level_id', r.level_id, 'time_ms', r.time_ms) order by r.level_id)
            from public.best_runs r join public.game_levels l on l.id = r.level_id where r.user_id = p.uid and l.active), '[]'::jsonb))
        into result from public.player_profiles p left join public.player_appearance a on a.user_id = p.uid where p.uid = p_uid;
    return result;
end $$;

revoke all on function public.require_social_user() from public, anon, authenticated;
revoke all on function public.ensure_player_profile(uuid) from public, anon, authenticated;
revoke all on function public.sync_auth_player_profile() from public, anon, authenticated;
revoke all on function public.broadcast_friend_change() from public, anon, authenticated;
revoke all on function public.get_friend_snapshot() from public, anon, authenticated;
revoke all on function public.friend_action(text, uuid, text, uuid) from public, anon, authenticated;
revoke all on function public.get_social_profile(uuid) from public, anon, authenticated;
grant execute on function public.get_friend_snapshot() to authenticated;
grant execute on function public.friend_action(text, uuid, text, uuid) to authenticated;
grant execute on function public.get_social_profile(uuid) to authenticated;
notify pgrst, 'reload schema';
commit;
