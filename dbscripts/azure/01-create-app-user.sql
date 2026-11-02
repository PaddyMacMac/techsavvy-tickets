-- OPTIONAL hardening. Run as the server admin, connected to the "tickets" database.
-- Replace CHANGE_ME with a long random password, then point DB_USER / DB_PASSWORD at this login.
create role tickets_app login password 'CHANGE_ME';
grant connect on database tickets to tickets_app;
grant usage, create on schema public to tickets_app;   -- Flyway needs create to run migrations
alter default privileges in schema public grant select, insert, update, delete on tables to tickets_app;
alter default privileges in schema public grant usage, select on sequences to tickets_app;
