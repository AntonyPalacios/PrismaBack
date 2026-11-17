-- Goals (REQ-GOALS-01, agreements of 2026-11-17): a tutor edits the goals
-- of their group, so `goal` becomes a mutable table and records who
-- changed it last, as every mutable table does.
ALTER TABLE goal
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN updated_by BIGINT REFERENCES app_user(id);
