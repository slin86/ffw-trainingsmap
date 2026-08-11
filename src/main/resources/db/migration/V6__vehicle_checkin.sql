CREATE TABLE vehicle_checkin (
    id              BIGSERIAL PRIMARY KEY,
    vehicle_id      BIGINT NOT NULL REFERENCES vehicle(id),
    username        VARCHAR NOT NULL,
    checked_in_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (username)
);
