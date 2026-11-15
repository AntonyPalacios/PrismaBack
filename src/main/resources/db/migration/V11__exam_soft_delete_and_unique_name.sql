-- Exams become manageable (REQ-EXAMS-01, agreements of 2026-11-15).

-- Soft delete, as in cycle and stage: an exam created by mistake (without
-- results) can be removed without touching the database by hand.
ALTER TABLE exam ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Names repeat every cycle (Medición, Nivelación, Regular 1-10, Repaso 1-5)
-- and a mock exam has one exam per type with the same name, so the name is
-- unique only within its stage and type, ignoring case.
CREATE UNIQUE INDEX ux_exam_stage_type_name_lower
    ON exam (stage_id, exam_type_id, lower(name)) WHERE NOT deleted;
