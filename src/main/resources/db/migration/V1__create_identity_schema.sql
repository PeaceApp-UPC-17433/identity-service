-- BC-01 Identity & Access: esquema inicial.

CREATE TABLE users (
    id                            UUID                     PRIMARY KEY,
    email                         VARCHAR(254)             NOT NULL,
    password_hash                 VARCHAR(100)             NOT NULL,
    display_name                  VARCHAR(80),
    role                          VARCHAR(20)              NOT NULL,
    jurisdiction                  VARCHAR(100),
    email_verified                BOOLEAN                  NOT NULL,
    verification_token_hash       VARCHAR(64),
    verification_token_expires_at TIMESTAMP WITH TIME ZONE,
    failed_login_attempts         INTEGER                  NOT NULL,
    locked_until                  TIMESTAMP WITH TIME ZONE,
    reputation_score              INTEGER                  NOT NULL,
    created_at                    TIMESTAMP WITH TIME ZONE NOT NULL,
    version                       BIGINT                   NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_verification_token UNIQUE (verification_token_hash),
    CONSTRAINT ck_users_role CHECK (role IN ('CIUDADANO', 'MODERADOR', 'INSTITUCIONAL', 'ADMINISTRADOR')),
    -- US-26: solo las cuentas institucionales tienen jurisdiccion, y siempre la tienen.
    CONSTRAINT ck_users_jurisdiction CHECK ((role = 'INSTITUCIONAL') = (jurisdiction IS NOT NULL))
);

CREATE TABLE interest_zones (
    id            UUID                     PRIMARY KEY,
    user_id       UUID                     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    label         VARCHAR(60)              NOT NULL,
    latitude      DOUBLE PRECISION         NOT NULL,
    longitude     DOUBLE PRECISION         NOT NULL,
    radius_meters INTEGER                  NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_interest_zones_user ON interest_zones (user_id);

CREATE TABLE refresh_tokens (
    id         UUID                     PRIMARY KEY,
    user_id    UUID                     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64)              NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

-- Transactional outbox: los eventos de dominio se guardan en la misma transaccion que el cambio
-- y un relay los publicara al broker cuando se defina (CA-01).
CREATE TABLE outbox_events (
    id           UUID                     PRIMARY KEY,
    aggregate_id UUID                     NOT NULL,
    event_type   VARCHAR(60)              NOT NULL,
    payload      VARCHAR(4000)            NOT NULL,
    occurred_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX ix_outbox_events_published ON outbox_events (published_at, occurred_at);
