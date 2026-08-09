-- Anonymous note-sharing schema.
-- All client operations go through Supabase Edge Functions using the service
-- role key. RLS is enabled with NO policies for the anon/authenticated roles,
-- which means direct browser access to the `notes` table is denied entirely.
-- This guarantees protected notes cannot be read, enumerated, or modified by
-- calling Supabase directly.

create extension if not exists pgcrypto;

-- The updated_at trigger function.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

create table public.notes (
    id            uuid primary key default gen_random_uuid(),
    public_id     text not null unique,
    type          text not null check (type in ('PUBLIC', 'PROTECTED')),
    name          text not null check (length(btrim(name)) between 1 and 200),
    description   text not null default '' check (length(description) <= 2000),
    content       text not null check (length(btrim(content)) between 1 and 20000),
    passcode_hash text,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),

    -- A PROTECTED note must always have a passcode hash.
    constraint protected_requires_passcode check (
        (type = 'PROTECTED' and passcode_hash is not null)
        or type = 'PUBLIC'
    ),
    -- A PUBLIC note must never carry a passcode hash.
    constraint public_forbids_passcode check (
        (type = 'PUBLIC' and passcode_hash is null)
        or type = 'PROTECTED'
    )
);

-- Homepage ordering: recent PUBLIC notes.
create index idx_notes_public_updated on public.notes (updated_at desc)
    where type = 'PUBLIC';

-- Lookup by URL identifier.
create index idx_notes_public_id on public.notes (public_id);

create trigger trg_notes_updated_at
    before update on public.notes
    for each row
    execute function public.set_updated_at();

-- Enable RLS. No policies are defined, so both the anon and authenticated
-- roles are denied all access. Only the service_role (used by Edge Functions)
-- can operate on the table, and service_role bypasses RLS by default.
alter table public.notes enable row level security;

revoke all on table public.notes from anon, authenticated;
grant all on table public.notes to service_role;
