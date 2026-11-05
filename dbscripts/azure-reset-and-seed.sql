-- DESTRUCTIVE: deletes all live tickets, requesters, and companies before seeding sample records.
-- Run this whole file in IntelliJ with the Azure `tickets` database connection selected.

begin;

truncate table public.tickets, public.requesters, public.companies restart identity cascade;
alter sequence public.ticket_reference_seq restart with 1000;

insert into public.companies (id, name)
values (gen_random_uuid(), 'Acme Bury Ltd'),
       (gen_random_uuid(), 'Lancashire Joinery');

insert into public.requesters (subject, name, email)
values ('8cad4976-5571-492e-9df0-be32a5f475a2', 'Paddy Azureman', 'paddyazureman@gmail.com');

insert into public.tickets (
    id, reference, title, description, category, priority, status, company_id,
    requester_subject, required_by, respond_by, created_at, updated_at, version
)
select gen_random_uuid(),
       'TSS-' || lpad(nextval('public.ticket_reference_seq')::text, 6, '0'),
       sample.title,
       sample.description,
       sample.category,
       sample.priority,
       sample.status,
       company.id,
       requester.subject,
       sample.required_by,
       now() + sample.respond_in,
       now() - sample.age,
       now() - sample.age,
       0
from (values
    ('New starter laptop for Alex',
     'Alex starts Monday. Needs a laptop with Microsoft 365 and the accounts package.',
     'NEW_STARTER', 'HIGH', 'OPEN', 'Acme Bury Ltd', current_date + 5, interval '3 hours', interval '1 hour'),
    ('Front office printer offline',
     'The printer next to reception has not worked since this morning.',
     'HARDWARE', 'URGENT', 'OPEN', 'Acme Bury Ltd', null::date, interval '-30 minutes', interval '2 hours'),
    ('Shared mailbox for orders',
     'Please create orders@ and give three people access.',
     'MICROSOFT_365', 'NORMAL', 'IN_PROGRESS', 'Acme Bury Ltd', null::date, interval '18 hours', interval '6 hours'),
    ('Office move: Bury to Ramsbottom',
     'We move in six weeks. Need a plan for the network and phones.',
     'OFFICE_MOVE', 'LOW', 'WAITING_ON_CLIENT', 'Lancashire Joinery', current_date + 42, interval '28 hours', interval '20 hours'),
    ('Review mailbox security settings',
     'Check the shared mailbox permissions after the recent staff changes.',
     'SECURITY', 'NORMAL', 'RESOLVED', 'Acme Bury Ltd', null::date, interval '72 hours', interval '48 hours')
) as sample(title, description, category, priority, status, company_name, required_by, respond_in, age)
join public.companies company on company.name = sample.company_name
cross join public.requesters requester;

commit;
