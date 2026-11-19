-- ForestFlow backend for Supabase (Postgres + Auth).
-- Run this whole file once: Supabase dashboard > SQL Editor > New query > paste > Run.
-- It is safe to run again (everything is "if not exists" / "or replace").
--
-- Auth settings to check as well (Authentication > Providers > Email):
--   * Email provider = ON
--   * "Confirm email" = OFF for the demo, so a new account can sign in straight away.
--     (If it stays ON, the app tells the user to confirm the e-mail first, then sign in.)

-- ---------------------------------------------------------------- profiles (role + name, one row per account)
create table if not exists public.profiles (
  id           uuid primary key references auth.users (id) on delete cascade,
  email        text,
  display_name text,
  role         text not null default 'Student' check (role in ('Student', 'Supplier')),
  created_at   timestamptz not null default now()
);

alter table public.profiles enable row level security;

drop policy if exists "profiles: read own row" on public.profiles;
create policy "profiles: read own row" on public.profiles
  for select to authenticated using (auth.uid() = id);

-- Create the profile row automatically from the sign-up metadata the app sends (role, display_name).
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, email, display_name, role)
  values (
    new.id,
    new.email,
    coalesce(nullif(new.raw_user_meta_data ->> 'display_name', ''), new.email),
    case when new.raw_user_meta_data ->> 'role' = 'Supplier' then 'Supplier' else 'Student' end
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------- user_state (each account's own app data)
create table if not exists public.user_state (
  user_id    uuid primary key references auth.users (id) on delete cascade default auth.uid(),
  state      jsonb  not null,
  client_ts  bigint not null default 0,          -- device clock (ms) when saved; the newer copy wins on sign-in
  updated_at timestamptz not null default now()
);

alter table public.user_state enable row level security;

drop policy if exists "user_state: own row" on public.user_state;
create policy "user_state: own row" on public.user_state
  for all to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- ---------------------------------------------------------------- activity_log (audit trail of meaningful actions)
create table if not exists public.activity_log (
  id         bigint generated always as identity primary key,
  user_id    uuid not null references auth.users (id) on delete cascade default auth.uid(),
  role       text not null,
  action     text not null,
  detail     text not null default '',
  created_at timestamptz not null default now()
);

create index if not exists activity_log_user_time on public.activity_log (user_id, created_at desc);

alter table public.activity_log enable row level security;

drop policy if exists "activity_log: read own" on public.activity_log;
create policy "activity_log: read own" on public.activity_log
  for select to authenticated using (auth.uid() = user_id);

drop policy if exists "activity_log: insert own" on public.activity_log;
create policy "activity_log: insert own" on public.activity_log
  for insert to authenticated with check (auth.uid() = user_id);

-- ---------------------------------------------------------------- delete my account (used by the app and its tests)
create or replace function public.delete_my_account()
returns void
language sql
security definer
set search_path = ''
as $$
  delete from auth.users where id = auth.uid();
$$;

revoke all on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;

-- ---------------------------------------------------------------- table privileges (Row Level Security does the filtering)
grant usage on schema public to anon, authenticated;
grant select on public.profiles to authenticated;
grant select, insert, update, delete on public.user_state to authenticated;
grant select, insert on public.activity_log to authenticated;
