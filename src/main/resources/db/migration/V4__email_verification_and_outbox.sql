ALTER TABLE users ADD COLUMN locale VARCHAR(5) NOT NULL DEFAULT 'es';

CREATE TABLE email_verifications (
    user_id      UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    code_hash    VARCHAR(64) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    attempts     INTEGER     NOT NULL DEFAULT 0,
    last_sent_at TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    version      BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY,
    routing_key  VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ
);
CREATE INDEX ix_outbox_pending ON outbox_events (created_at) WHERE published_at IS NULL;
