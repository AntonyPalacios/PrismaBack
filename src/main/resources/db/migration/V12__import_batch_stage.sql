-- Student imports can enroll their students in a stage when the batch is
-- confirmed (agreements of 2026-11-15); null = only register them.
ALTER TABLE import_batch ADD COLUMN stage_id BIGINT REFERENCES stage(id);
