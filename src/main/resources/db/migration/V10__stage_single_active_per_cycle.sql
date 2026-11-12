-- Only one stage of a cycle can be ACTIVE at a time. The service checks it
-- first to answer a clear 409; this partial index is the database guarantee.
CREATE UNIQUE INDEX ux_stage_single_active_per_cycle ON stage (cycle_id) WHERE status = 'ACTIVE' AND NOT deleted;
