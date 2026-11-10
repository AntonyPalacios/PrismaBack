-- Career names are unique ignoring case ("biomédica" collides with
-- "Biomédica"). The service checks it first to answer a clear 409; this
-- index is the database-level guarantee. V1 had no uniqueness on career.
CREATE UNIQUE INDEX ux_career_name_lower ON career (lower(name));
