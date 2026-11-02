CREATE TABLE antag_tokens(
    discord_id BIGINT PRIMARY KEY,
    balance INT NOT NULL DEFAULT 0, CHECK (balance >= 0),
    last_weekly_grant_at TIMESTAMPTZ
);
