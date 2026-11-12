-- Only one cycle can be ACTIVE at a time. The service checks it first to
-- answer a clear 409; this partial index is the database-level guarantee.
CREATE UNIQUE INDEX ux_cycle_single_active ON cycle (status) WHERE status = 'ACTIVE';
