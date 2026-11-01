CREATE TABLE boosty_subscription (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    discord_id BIGINT NOT NULL,
    promoted_by_id BIGINT NOT NULL,
    rank INT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at TIMESTAMPTZ
);

INSERT INTO boosty_subscription (discord_id, promoted_by_id, rank, started_at)
SELECT discord_id, promoted_by_id, rank, started_at
FROM active_boosty_subscriber;

INSERT INTO boosty_subscription (discord_id, promoted_by_id, rank, started_at, ended_at)
SELECT discord_id, promoted_by_id, rank, started_at, expire_at
FROM expired_boosty_subscriber;

CREATE UNIQUE INDEX uq_boosty_equal_rank ON boosty_subscription (discord_id, rank) WHERE ended_at IS NULL;

DROP TABLE active_boosty_subscriber;
DROP TABLE expired_boosty_subscriber;
