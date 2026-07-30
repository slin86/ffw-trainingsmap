CREATE TABLE scene (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR NOT NULL,
    description  TEXT,
    lat          DOUBLE PRECISION NOT NULL,
    lng          DOUBLE PRECISION NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE scene_vehicle (
    scene_id     BIGINT NOT NULL REFERENCES scene(id) ON DELETE CASCADE,
    vehicle_id   BIGINT NOT NULL REFERENCES vehicle(id),
    PRIMARY KEY (scene_id, vehicle_id)
);
