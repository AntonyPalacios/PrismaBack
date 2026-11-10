-- Exam type names are unique ignoring case ("ciencias" collides with
-- "Ciencias"). The service checks it first to answer a clear 409; this
-- index is the database-level guarantee. It replaces V1's case-sensitive
-- UNIQUE constraint on name.
ALTER TABLE exam_type DROP CONSTRAINT exam_type_name_key;
CREATE UNIQUE INDEX ux_exam_type_name_lower ON exam_type (lower(name));
