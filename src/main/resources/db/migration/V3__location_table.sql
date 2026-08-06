CREATE TABLE location (
    id           BIGSERIAL PRIMARY KEY,
    location_type VARCHAR(20) NOT NULL,
    name         VARCHAR NOT NULL,
    lat          DOUBLE PRECISION NOT NULL,
    lng          DOUBLE PRECISION NOT NULL,
    active       BOOLEAN DEFAULT true,
    description  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_location_lat CHECK (lat >= 53.3 AND lat <= 53.8),
    CONSTRAINT chk_location_lng CHECK (lng >= 9.6 AND lng <= 10.4)
);

ALTER TABLE vehicle
ADD COLUMN location_id BIGINT REFERENCES location(id);
