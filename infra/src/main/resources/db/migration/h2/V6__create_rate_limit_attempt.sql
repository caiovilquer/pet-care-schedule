-- Variante H2 da V6 (TIMESTAMPTZ não existe no H2)
CREATE TABLE IF NOT EXISTS rate_limit_attempt (
    id VARCHAR(200) PRIMARY KEY,
    count INTEGER NOT NULL,
    window_start TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT
);
