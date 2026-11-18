-- À exécuter une fois dans SQL Editor (projet Supabase personnel).
-- Aucune clé de service dans le client. Auth Google + RPC bornées ; écritures directes interdites.
begin;

create table if not exists public.game_levels (
    id text primary key,
    version integer not null check (version > 0),
    active boolean not null default true,
    min_time_ms bigint not null default 10000,
    max_time_ms bigint not null default 600000,
    check (min_time_ms > 0 and max_time_ms >= min_time_ms)
);
insert into public.game_levels (id, version) values
    ('prairie-1-v2', 2), ('canyon-2-v1', 1), ('night-3-v1', 1)
on conflict (id) do nothing;

create table if not exists public.best_runs (
    level_id text not null references public.game_levels(id),
    user_id uuid not null references auth.users(id) on delete cascade,
    time_ms bigint not null check (time_ms between 10000 and 600000),
    coins integer not null check (coins between 0 and 100),
    display_name text not null check (char_length(display_name) between 1 and 40),
    updated_at timestamptz not null default now(),
    primary key (level_id, user_id)
);
create index if not exists best_runs_ranking_idx on public.best_runs (level_id, time_ms, user_id);

alter table public.game_levels enable row level security;
alter table public.best_runs enable row level security;
revoke all on table public.game_levels, public.best_runs from public, anon, authenticated;
-- Défense supplémentaire si une permission SELECT était accordée ultérieurement.
drop policy if exists google_ranking_read on public.best_runs;
create policy google_ranking_read on public.best_runs for select to authenticated
using ((select auth.uid()) is not null and (select auth.jwt()) -> 'app_metadata' ->> 'provider' = 'google');
-- Aucune policy INSERT/UPDATE/DELETE : seul submit_run peut écrire pour auth.uid().

create or replace function public.submit_run(p_level_id text, p_level_version integer, p_time_ms bigint, p_coins integer)
returns void language plpgsql security definer set search_path = '' as $$
declare
    player_id uuid := auth.uid();
    level_row public.game_levels%rowtype;
    player_name text;
begin
    if player_id is null or coalesce(auth.jwt() -> 'app_metadata' ->> 'provider', '') <> 'google' then
        raise exception 'Google authentication required' using errcode = '42501';
    end if;
    select * into level_row from public.game_levels where id = p_level_id and active;
    if not found or p_level_version is null or p_level_version <> level_row.version
        or p_time_ms is null or p_time_ms < level_row.min_time_ms or p_time_ms > level_row.max_time_ms
        or p_coins is null or p_coins < 0 or p_coins > 100 then
        raise exception 'Invalid run' using errcode = '22023';
    end if;
    select left(trim(regexp_replace(coalesce(raw_user_meta_data ->> 'full_name', raw_user_meta_data ->> 'name', ''),
        '[[:cntrl:]]', '', 'g')), 40) into player_name from auth.users where id = player_id;
    if not found then raise exception 'Unknown user' using errcode = '42501'; end if;
    player_name := coalesce(nullif(player_name, ''), 'Joueur');
    insert into public.best_runs (level_id, user_id, time_ms, coins, display_name, updated_at)
    values (p_level_id, player_id, p_time_ms, p_coins, player_name, now())
    on conflict (level_id, user_id) do update
        set time_ms = excluded.time_ms, coins = excluded.coins,
            display_name = excluded.display_name, updated_at = now()
        where excluded.time_ms < public.best_runs.time_ms;
end;
$$;

create or replace function public.get_leaderboard(p_level_id text)
returns table(uid uuid, display_name text, time_ms bigint, coins integer)
language plpgsql security definer set search_path = '' as $$
begin
    if auth.uid() is null or coalesce(auth.jwt() -> 'app_metadata' ->> 'provider', '') <> 'google' then
        raise exception 'Google authentication required' using errcode = '42501';
    end if;
    if not exists (select 1 from public.game_levels where id = p_level_id and active) then
        raise exception 'Unknown level' using errcode = '22023';
    end if;
    return query select r.user_id, r.display_name, r.time_ms, r.coins
        from public.best_runs r where r.level_id = p_level_id order by r.time_ms, r.user_id limit 50;
end;
$$;

revoke all on function public.submit_run(text, integer, bigint, integer) from public, anon, authenticated;
revoke all on function public.get_leaderboard(text) from public, anon, authenticated;
grant execute on function public.submit_run(text, integer, bigint, integer) to authenticated;
grant execute on function public.get_leaderboard(text) to authenticated;
notify pgrst, 'reload schema';
commit;
