create table if not exists refresh_token (
    id uuid primary key,
    token_hash varchar(64) not null unique,
    user_id BIGINT not null,
    family_id uuid not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null default now(),
    used_at timestamp with time zone null,
    revoked_at timestamp with time zone null,
    replaced_by uuid null,
    user_agent varchar(256) null
    );
create index if not exists idx_rt_user on refresh_token(user_id);
create index if not exists idx_rt_family on refresh_token(family_id);
create index if not exists idx_rt_expires on refresh_token(expires_at);
