-- Mock exams (agreements of 2026-11-15, replaces "there is no mock exam
-- entity" of REQ-EXAMS-01). A mock exam (simulacro) is what the academy
-- schedules: a name and a date in a stage. Each of its exam types is one
-- `exam`, which keeps its own results. Renaming or moving the date of a mock
-- exam therefore applies to all its types at once.

CREATE TABLE mock_exam (
    id          BIGSERIAL PRIMARY KEY,
    stage_id    BIGINT NOT NULL REFERENCES stage(id),
    name        VARCHAR(200) NOT NULL,
    exam_date   DATE NOT NULL,
    -- soft delete, as in cycle and stage: only while none of its exams has results
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT REFERENCES app_user(id),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  BIGINT REFERENCES app_user(id)
);
CREATE INDEX ix_mock_exam_stage ON mock_exam (stage_id);

-- Names repeat every cycle (Medición, Nivelación, Regular 1-10, Repaso 1-5),
-- so the name is unique only within its stage, ignoring case.
CREATE UNIQUE INDEX ux_mock_exam_stage_name_lower ON mock_exam (stage_id, lower(name)) WHERE NOT deleted;

-- Any exam created before this migration becomes part of a mock exam: one
-- per stage, name and date (the convention REQ-EXAMS-01 relied on).
ALTER TABLE exam ADD COLUMN mock_exam_id BIGINT REFERENCES mock_exam(id);
INSERT INTO mock_exam (stage_id, name, exam_date, created_at, created_by, updated_at, updated_by)
    SELECT stage_id, name, exam_date, min(created_at), min(created_by), max(updated_at), max(updated_by)
    FROM exam
    GROUP BY stage_id, name, exam_date;
UPDATE exam e SET mock_exam_id = m.id
    FROM mock_exam m
    WHERE m.stage_id = e.stage_id AND m.name = e.name AND m.exam_date = e.exam_date;
ALTER TABLE exam ALTER COLUMN mock_exam_id SET NOT NULL;
CREATE INDEX ix_exam_mock_exam ON exam (mock_exam_id);

-- Stage, name and date now live in the mock exam.
DROP INDEX ix_exam_stage;
ALTER TABLE exam DROP COLUMN stage_id;
ALTER TABLE exam DROP COLUMN name;
ALTER TABLE exam DROP COLUMN exam_date;

-- An exam type can be removed from a mock exam (soft) while it has no
-- results; a mock exam has each type at most once.
ALTER TABLE exam ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX ux_exam_mock_exam_type ON exam (mock_exam_id, exam_type_id) WHERE NOT deleted;
