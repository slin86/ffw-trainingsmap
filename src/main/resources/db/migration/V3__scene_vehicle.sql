ALTER TABLE public.vehicle ADD COLUMN scene_id BIGINT REFERENCES scene(id);
