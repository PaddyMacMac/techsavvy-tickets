-- Demo data for local development only. Safe to run more than once (it simply adds more rows).
-- Requester subjects match the local demo logins so those accounts can see their tickets.
insert into companies (id, name)
values (gen_random_uuid(), 'Acme Bury Ltd'),
       (gen_random_uuid(), 'Lancashire Joinery')
on conflict (name) do nothing;

insert into requesters (subject, name, email)
values ('client@acme.local', 'Demo Client', 'client@acme.local'),
       ('owner@joinery.local', 'Pat Owner', 'owner@joinery.local')
on conflict (subject) do update set name  = excluded.name,
                                    email = excluded.email;

insert into tickets (id, reference, title, description, category, priority, status, company_id,
                     requester_subject, required_by, respond_by, created_at, updated_at, version)
select gen_random_uuid(),
       'TSS-' || lpad(nextval('ticket_reference_seq')::text, 6, '0'),
       t.title,
       t.description,
       t.category,
       t.priority,
       t.status,
       c.id,
       t.subject,
       t.required_by,
       now() + t.respond_in,
       now() - t.age,
       now() - t.age,
       0
from (values ('New starter laptop for Casey',
              'Alex starts Monday. Needs a laptop with Microsoft 365 and the accounts package.',
              'NEW_STARTER', 'HIGH', 'OPEN', 'Acme Bury Ltd', 'client@acme.local', current_date + 5, interval '2 hours',
              interval '1 hour'),
             ('Front office printer offline', 'The printer next to reception has not worked since this morning.',
              'HARDWARE', 'URGENT', 'OPEN', 'Acme Bury Ltd', 'client@acme.local', null::date, interval '-30 minutes',
              interval '90 minutes'),
             ('Shared mailbox for orders', 'Please create orders@ and give three people access.',
              'MICROSOFT_365', 'NORMAL', 'IN_PROGRESS', 'Acme Bury Ltd', 'client@acme.local', null::date,
              interval '6 hours', interval '1 day'),
             ('Office move: Bury to Ramsbottom', 'We move in six weeks. Need a plan for the network and phones.',
              'OFFICE_MOVE', 'LOW', 'WAITING_ON_CLIENT', 'Lancashire Joinery', 'owner@joinery.local', current_date + 42,
              interval '20 hours', interval '2 days')) as t(title, description, category, priority, status, company,
                                                            subject, required_by, respond_in, age)
         join companies c on c.name = t.company;
