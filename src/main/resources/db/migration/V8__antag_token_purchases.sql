CREATE TABLE antag_token_purchase (
    id UUID PRIMARY KEY,
    discord_id BIGINT NOT NULL,
    listing_id VARCHAR(64) NOT NULL,
    server_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ,
    CONSTRAINT chk_antag_token_purchase_status CHECK (status IN ('PENDING', 'GRANTED', 'REFUNDED'))
);

-- A player can have only one purchase waiting for a round.
CREATE UNIQUE INDEX uq_antag_token_purchase_active ON antag_token_purchase (discord_id) WHERE status = 'PENDING';
CREATE INDEX idx_antag_token_purchase_server_pending ON antag_token_purchase (server_id) WHERE status = 'PENDING';
