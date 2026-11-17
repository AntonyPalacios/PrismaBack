-- Forced reload of an exam's results (agreements of 2026-11-16, REQ-EXAMS-03):
-- an admin uploads the file again for an exam that already has results. The
-- batch remembers it is a reload, so confirming it deletes the exam's
-- results and saves the new ones in the same transaction.
ALTER TABLE import_batch ADD COLUMN replaces_results BOOLEAN NOT NULL DEFAULT FALSE;
