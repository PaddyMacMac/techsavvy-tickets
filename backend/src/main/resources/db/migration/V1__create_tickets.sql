-- Human-friendly references: TSS-001000, TSS-001001, ...
create sequence ticket_reference_seq start with 1000 increment by 1;

create table tickets (
    id                uuid         primary key,
    reference         varchar(20)  not null unique,
    title             varchar(150) not null,
    description       text         not null,
    category          varchar(30)  not null,
    priority          varchar(20)  not null,
    status            varchar(30)  not null,
    company_name      varchar(120) not null,
    requester_subject varchar(100) not null,
    requester_name    varchar(120) not null,
    requester_email   varchar(200) not null,
    required_by       date,
    respond_by        timestamptz  not null,
    created_at        timestamptz  not null,
    updated_at        timestamptz  not null,
    version           bigint       not null default 0,
    constraint ck_tickets_category check (category in
        ('NEW_STARTER','HARDWARE','SOFTWARE','MICROSOFT_365','SECURITY','NETWORK','OFFICE_MOVE','OTHER')),
    constraint ck_tickets_priority check (priority in ('LOW','NORMAL','HIGH','URGENT')),
    constraint ck_tickets_status check (status in
        ('OPEN','IN_PROGRESS','WAITING_ON_CLIENT','RESOLVED','CLOSED'))
);

create index ix_tickets_status_created   on tickets (status, created_at desc);
create index ix_tickets_requester        on tickets (requester_subject, created_at desc);
create index ix_tickets_open_respond_by  on tickets (respond_by) where status = 'OPEN';
