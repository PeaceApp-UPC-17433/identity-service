ALTER TABLE users
    ADD COLUMN mfa_enabled         BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN mfa_secret          VARCHAR(200),
    ADD COLUMN mfa_pending_secret  VARCHAR(200),
    ADD COLUMN mfa_last_step       BIGINT      NOT NULL DEFAULT 0,
    ADD COLUMN mfa_failed_attempts INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN mfa_locked_until    TIMESTAMPTZ;

CREATE TABLE mfa_recovery_codes (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash  VARCHAR(64) NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_mfa_recovery_user ON mfa_recovery_codes (user_id);
