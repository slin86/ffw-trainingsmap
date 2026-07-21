CREATE TABLE vehicle (
    id          BIGSERIAL PRIMARY KEY,
    callsign    VARCHAR NOT NULL UNIQUE,
    type        VARCHAR NOT NULL,
    status      INT NOT NULL DEFAULT 2,
    lat         DOUBLE PRECISION NOT NULL,
    lng         DOUBLE PRECISION NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR NOT NULL UNIQUE,
    password_hash VARCHAR NOT NULL,
    role          VARCHAR NOT NULL,
    enabled       BOOLEAN NOT NULL DEFAULT true
);
