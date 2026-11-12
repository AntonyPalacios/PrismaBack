-- Stages become manageable (agreements of 2026-11-12). V1 only had the bare
-- table; this brings it in line with cycle (V6–V8).

-- A stage is identified by its name, not by a system-assigned number.
-- (Dropping the column also drops UNIQUE (cycle_id, sequence).)
ALTER TABLE stage DROP COLUMN sequence;

-- The start date is required; the end date stays optional (open-ended).
ALTER TABLE stage ALTER COLUMN start_date SET NOT NULL;
ALTER TABLE stage
    ADD CONSTRAINT ck_stage_end_not_before_start
    CHECK (end_date IS NULL OR end_date >= start_date);

-- Same states as a cycle, forward only.
ALTER TABLE stage
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PLANNED'
    CHECK (status IN ('PLANNED', 'ACTIVE', 'CLOSED'));

-- Soft delete, as in cycle: a deleted stage stops counting for every rule below.
ALTER TABLE stage ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- The name is unique within its cycle, ignoring case.
CREATE UNIQUE INDEX ux_stage_cycle_name_lower ON stage (cycle_id, lower(name)) WHERE NOT deleted;

-- Stages of the same cycle never overlap ('[]': they can't share even one
-- day; no end date = open-ended). Comparing cycle_id with "=" inside a gist
-- constraint needs btree_gist, which ships with PostgreSQL.
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE stage
    ADD CONSTRAINT ex_stage_no_overlap
    EXCLUDE USING gist (cycle_id WITH =, daterange(start_date, end_date, '[]') WITH &&)
    WHERE (NOT deleted);
