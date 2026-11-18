-- Vérifications de structure en lecture seule, à lancer après la migration dans SQL Editor.
select id, version, active, theme, difficulty from public.game_levels order by theme, difficulty, id;
select relname, relrowsecurity from pg_class where oid in ('public.game_levels'::regclass, 'public.best_runs'::regclass);
-- Les quatre colonnes de permissions directes ci-dessous doivent être false.
select role_name, has_table_privilege(role_name, 'public.best_runs', 'SELECT') as direct_select,
    has_table_privilege(role_name, 'public.best_runs', 'INSERT') as direct_insert,
    has_table_privilege(role_name, 'public.best_runs', 'UPDATE') as direct_update,
    has_table_privilege(role_name, 'public.best_runs', 'DELETE') as direct_delete
from (values ('anon'), ('authenticated')) roles(role_name);
-- false pour anon ; true pour authenticated.
select role_name, has_function_privilege(role_name, 'public.submit_run(text,integer,bigint,integer)', 'EXECUTE') as submit,
    has_function_privilege(role_name, 'public.get_leaderboard(text)', 'EXECUTE') as ranking
from (values ('anon'), ('authenticated')) roles(role_name);
select proname, prosecdef, proconfig from pg_proc where oid in
    ('public.submit_run(text,integer,bigint,integer)'::regprocedure, 'public.get_leaderboard(text)'::regprocedure);
-- Après la migration 202611180001 : apparence protégée et deux nouvelles RPC.
select relname, relrowsecurity from pg_class where oid = 'public.player_appearance'::regclass;
select role_name, has_table_privilege(role_name, 'public.player_appearance', 'SELECT') as direct_select,
    has_table_privilege(role_name, 'public.player_appearance', 'INSERT') as direct_insert,
    has_table_privilege(role_name, 'public.player_appearance', 'UPDATE') as direct_update,
    has_table_privilege(role_name, 'public.player_appearance', 'DELETE') as direct_delete,
    has_function_privilege(role_name, 'public.get_player_progress()', 'EXECUTE') as progress,
    has_function_privilege(role_name, 'public.equip_skin(text)', 'EXECUTE') as equip
from (values ('anon'), ('authenticated')) roles(role_name);
select proname, prosecdef, proconfig from pg_proc where oid in
    ('public.get_player_progress()'::regprocedure, 'public.equip_skin(text)'::regprocedure);
