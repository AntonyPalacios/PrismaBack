-- An admin can delete a cycle created by mistake (soft delete: the row stays,
-- flagged). A deleted cycle must stop counting for every cycle rule, so the
-- constraints of V6 and V7 are rebuilt to ignore deleted rows: its name, its
-- dates and the single ACTIVE slot become free again.
ALTER TABLE cycle ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

DROP INDEX ux_cycle_name_lower;
CREATE UNIQUE INDEX ux_cycle_name_lower ON cycle (lower(name)) WHERE NOT deleted;

ALTER TABLE cycle DROP CONSTRAINT ex_cycle_no_overlap;
ALTER TABLE cycle
    ADD CONSTRAINT ex_cycle_no_overlap
    EXCLUDE USING gist (daterange(start_date, end_date, '[]') WITH &&)
    WHERE (NOT deleted);

DROP INDEX ux_cycle_single_active;
CREATE UNIQUE INDEX ux_cycle_single_active ON cycle (status) WHERE status = 'ACTIVE' AND NOT deleted;
