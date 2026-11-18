-- Extension additive : conserver tous les parcours et records déjà publiés.
begin;

alter table public.game_levels add column if not exists theme text not null default 'meadow'
    check (theme in ('meadow', 'canyon', 'night'));
alter table public.game_levels add column if not exists difficulty text not null default 'easy'
    check (difficulty in ('easy', 'normal', 'expert'));
update public.game_levels set theme = 'canyon', difficulty = 'normal' where id = 'canyon-2-v1';
update public.game_levels set theme = 'night', difficulty = 'expert' where id = 'night-3-v1';

insert into public.game_levels (id, version, theme, difficulty) values
    ('prairie-trails-v1', 1, 'meadow', 'normal'),
    ('prairie-summits-v1', 1, 'meadow', 'expert'),
    ('canyon-oasis-v1', 1, 'canyon', 'easy'),
    ('canyon-ridges-v1', 1, 'canyon', 'expert'),
    ('night-lanterns-v1', 1, 'night', 'easy'),
    ('night-observatory-v1', 1, 'night', 'normal')
on conflict (id) do nothing;

create table if not exists public.player_appearance (
    user_id uuid primary key references auth.users(id) on delete cascade,
    skin_id text not null default 'luigi' check (skin_id in ('luigi', 'toad', 'yoshi', 'peach')),
    updated_at timestamptz not null default now()
);
alter table public.player_appearance enable row level security;
revoke all on table public.player_appearance from public, anon, authenticated;

-- Les missions en ligne sont déduites des records du joueur, jamais d'un compteur envoyé par le client.
create or replace function public.get_player_progress()
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    player_id uuid := auth.uid();
    completed text[];
    equipped text;
begin
    if player_id is null or coalesce(auth.jwt() -> 'app_metadata' ->> 'provider', '') <> 'google'
        or not exists (select 1 from auth.users where id = player_id) then
        raise exception 'Google authentication required' using errcode = '42501';
    end if;
    select coalesce(array_agg(r.level_id order by r.level_id), array[]::text[]) into completed
        from public.best_runs r join public.game_levels l on l.id = r.level_id
        where r.user_id = player_id and l.active;
    select skin_id into equipped from public.player_appearance where user_id = player_id;
    return jsonb_build_object('completed_levels', completed, 'equipped_skin', coalesce(equipped, 'luigi'));
end;
$$;

create or replace function public.equip_skin(p_skin_id text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    player_id uuid := auth.uid();
    finished integer;
    worlds integer;
    experts integer;
begin
    -- Valide également l'existence du compte avant toute écriture.
    perform public.get_player_progress();
    if p_skin_id is null or p_skin_id not in ('luigi', 'toad', 'yoshi', 'peach') then
        raise exception 'Unknown skin' using errcode = '22023';
    end if;
    select count(*), count(distinct l.theme), count(*) filter (where l.difficulty = 'expert')
        into finished, worlds, experts
        from public.best_runs r join public.game_levels l on l.id = r.level_id
        where r.user_id = player_id and l.active;
    if (p_skin_id = 'toad' and finished < 1) or (p_skin_id = 'yoshi' and worlds < 3)
        or (p_skin_id = 'peach' and experts < 3) then
        raise exception 'Skin is locked' using errcode = '42501';
    end if;
    insert into public.player_appearance (user_id, skin_id) values (player_id, p_skin_id)
        on conflict (user_id) do update set skin_id = excluded.skin_id, updated_at = now();
    return public.get_player_progress();
end;
$$;

revoke all on function public.get_player_progress() from public, anon, authenticated;
revoke all on function public.equip_skin(text) from public, anon, authenticated;
grant execute on function public.get_player_progress() to authenticated;
grant execute on function public.equip_skin(text) to authenticated;
notify pgrst, 'reload schema';
commit;
