create table if not exists password_reset_token (
    id uuid primary key,
    token_hash varchar(64) not null unique,
    user_id BIGINT not null,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone null,
    created_at timestamp with time zone not null default now()
    );
create index if not exists idx_prt_user on password_reset_token(user_id);
create index if not exists idx_prt_expires on password_reset_token(expires_at);
