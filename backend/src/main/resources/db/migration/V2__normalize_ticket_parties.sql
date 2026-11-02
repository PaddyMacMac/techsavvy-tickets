create table companies (
    id   uuid         primary key,
    name varchar(120) not null unique
);

create table requesters (
    subject varchar(100)  primary key,
    name    varchar(120)  not null,
    email   varchar(200)  not null
);

insert into companies (id, name)
select gen_random_uuid(), company_name
from tickets
group by company_name;

insert into requesters (subject, name, email)
select distinct on (requester_subject) requester_subject, requester_name, requester_email
from tickets
order by requester_subject, updated_at desc, created_at desc, id;

alter table tickets add column company_id uuid;

update tickets t
set company_id = c.id
from companies c
where c.name = t.company_name;

alter table tickets alter column company_id set not null;
alter table tickets add constraint fk_tickets_company
    foreign key (company_id) references companies (id);
alter table tickets add constraint fk_tickets_requester
    foreign key (requester_subject) references requesters (subject);

alter table tickets drop column company_name;
alter table tickets drop column requester_name;
alter table tickets drop column requester_email;

