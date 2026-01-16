CREATE TABLE IF NOT EXISTS rate_limit_attempt (
    id VARCHAR(200) PRIMARY KEY,
    count INTEGER NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    version BIGINT
);
