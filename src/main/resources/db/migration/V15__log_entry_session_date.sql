-- Logbook (REQ-LOGBOOK-01/02, agreements of 2026-11-17): an entry records
-- the day of the session, which may be earlier than the day it is written,
-- and who the tutor met: the student, their parent or guardian, or both.
-- Existing rows (if any) take the day they were written, in the academy
-- zone, and the student.
ALTER TABLE log_entry ADD COLUMN session_date DATE;
UPDATE log_entry SET session_date = (created_at AT TIME ZONE 'America/Lima')::date;
ALTER TABLE log_entry ALTER COLUMN session_date SET NOT NULL;

ALTER TABLE log_entry ADD COLUMN meeting_with VARCHAR(10) NOT NULL DEFAULT 'STUDENT'
    CHECK (meeting_with IN ('STUDENT', 'PARENT', 'BOTH'));
ALTER TABLE log_entry ALTER COLUMN meeting_with DROP DEFAULT;

-- the history is read newest session first
DROP INDEX ix_log_entry_student;
CREATE INDEX ix_log_entry_student ON log_entry (student_id, session_date DESC, id DESC);
