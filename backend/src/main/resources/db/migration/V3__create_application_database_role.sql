do $$
declare
    object_record record;
    bootstrap_user name := current_user;
begin
    execute format('create role tickets_app login password %L', '${ticketsAppPassword}');
    execute format('grant tickets_app to %I', bootstrap_user);
    execute 'grant usage, create on schema public to tickets_app';

    for object_record in
        select tablename as object_name
        from pg_tables
        where schemaname = 'public'
          and tablename <> 'flyway_schema_history'
    loop
        execute format('alter table public.%I owner to tickets_app', object_record.object_name);
    end loop;

    for object_record in
        select sequencename as object_name
        from pg_sequences
        where schemaname = 'public'
    loop
        execute format('alter sequence public.%I owner to tickets_app', object_record.object_name);
    end loop;

    execute format('grant connect on database %I to tickets_app', current_database());
    execute 'grant select, insert, update, delete on table public.flyway_schema_history to tickets_app';
    execute format('revoke tickets_app from %I', bootstrap_user);
end $$;
