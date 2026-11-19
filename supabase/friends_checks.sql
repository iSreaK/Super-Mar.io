-- Lecture seule, à exécuter après la migration sociale dans Supabase SQL Editor.
-- Ce contrôle ne simule aucun utilisateur et ne crée ni demande ni compte.

select c.relname, c.relrowsecurity as rls_enabled
from pg_class c join pg_namespace n on n.oid = c.relnamespace
where (n.nspname = 'public' and c.relname in ('player_profiles', 'friend_relations'))
   or (n.nspname = 'realtime' and c.relname = 'messages');

select p.oid::regprocedure as function, p.prosecdef as security_definer, p.proconfig,
    has_function_privilege('anon', p.oid, 'execute') as anon_execute,
    has_function_privilege('authenticated', p.oid, 'execute') as authenticated_execute
from pg_proc p join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public' and p.proname in ('get_friend_snapshot', 'friend_action', 'get_social_profile',
    'require_social_user', 'ensure_player_profile', 'sync_auth_player_profile', 'broadcast_friend_change');

select name, has_table_privilege('anon', 'public.' || name, 'select,insert,update,delete') as anon_direct_access,
    has_table_privilege('authenticated', 'public.' || name, 'select,insert,update,delete') as authenticated_direct_access
from (values ('player_profiles'), ('friend_relations')) as tables(name);

select schemaname, tablename, policyname, permissive, cmd, roles, qual, with_check
from pg_policies where (schemaname = 'realtime' and policyname like 'mariosprint_friends_%')
    or (schemaname = 'public' and tablename in ('player_profiles', 'friend_relations'))
order by schemaname, tablename, policyname;

select tgname, tgenabled from pg_trigger
where not tgisinternal and tgname in ('mariosprint_player_profile', 'mariosprint_friend_change');

select conname, pg_get_constraintdef(oid) as definition from pg_constraint
where conrelid = 'public.friend_relations'::regclass order by conname;
