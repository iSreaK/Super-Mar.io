-- Vérifications de structure en lecture seule, à lancer après la migration dans SQL Editor.
select id, version, active from public.game_levels order by id;
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
