-- Cycle rules guaranteed by the database (the service checks them first to
-- answer a clear 400/409; these are the safety net). V1 had none of them.

-- Names are unique ignoring case ("2026-i" collides with "2026-I").
CREATE UNIQUE INDEX ux_cycle_name_lower ON cycle (lower(name));

-- A cycle can't end before it starts.
ALTER TABLE cycle
    ADD CONSTRAINT ck_cycle_end_not_before_start
    CHECK (end_date IS NULL OR end_date >= start_date);

-- Cycles never overlap. '[]' makes both ends inclusive, so two cycles can't
-- share even one day; a cycle without end date is open-ended (it overlaps
-- with everything that starts after it).
ALTER TABLE cycle
    ADD CONSTRAINT ex_cycle_no_overlap
    EXCLUDE USING gist (daterange(start_date, end_date, '[]') WITH &&);
