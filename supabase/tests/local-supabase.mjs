// Contrats Auth/Realtime minimaux pour Postgres local. Aucun service distant n'est appelé.
export async function createLocalSupabase(database) {
    await database.exec(`create role anon; create role authenticated; create schema auth; create schema realtime;
        create table auth.users(id uuid primary key, raw_user_meta_data jsonb);
        create function auth.jwt() returns jsonb language sql stable as $$
            select coalesce(nullif(current_setting('request.jwt.claims',true),''),'{}')::jsonb $$;
        create function auth.uid() returns uuid language sql stable as $$ select (auth.jwt()->>'sub')::uuid $$;
        create function realtime.topic() returns text language sql stable as $$ select current_setting('realtime.topic',true) $$;
        create table realtime.messages(topic text not null, extension text not null, payload jsonb, event text, private boolean);
        alter table realtime.messages enable row level security;
        create function realtime.send(payload jsonb, event text, topic text, private boolean) returns void
            language sql as $$ insert into realtime.messages values(topic,'broadcast',payload,event,private) $$;
        grant usage on schema auth, realtime to anon, authenticated;
        grant select, insert on realtime.messages to authenticated;`);
}
