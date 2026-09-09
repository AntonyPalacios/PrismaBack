-- =====================================================================
-- Prisma v2 — baseline schema
-- Mirrors the ER model agreed on 2026-08-21 (see prisma-v2 architecture
-- notes). Every table that needs "who touched this last" carries
-- created_by/created_at/updated_by/updated_at, populated by Spring Data
-- JPA auditing — never by application code setting them by hand.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Users & roles
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    first_name      VARCHAR(150) NOT NULL,
    last_name_pat   VARCHAR(100) NOT NULL,
    last_name_mat   VARCHAR(100),
    google_sub      VARCHAR(255),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT REFERENCES app_user(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT REFERENCES app_user(id)
);
CREATE UNIQUE INDEX ux_app_user_email ON app_user (email);
CREATE UNIQUE INDEX ux_app_user_google_sub ON app_user (google_sub) WHERE google_sub IS NOT NULL;

CREATE TABLE role (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(50) NOT NULL UNIQUE -- ADMIN, TUTOR
);

CREATE TABLE user_role (
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ---------------------------------------------------------------------
-- Catalog — editable from the admin UI, not hardcoded enums (RF-03.01/02)
-- ---------------------------------------------------------------------
CREATE TABLE area (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    abbreviation    VARCHAR(20),
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE course (
    id              BIGSERIAL PRIMARY KEY,
    area_id         BIGINT NOT NULL REFERENCES area(id),
    name            VARCHAR(100) NOT NULL,
    abbreviation    VARCHAR(20),
    active          BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX ix_course_area ON course (area_id);

CREATE TABLE exam_type (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(50) NOT NULL UNIQUE, -- Ciencias, Letras, Arquitectura
    active  BOOLEAN NOT NULL DEFAULT TRUE
);

-- which courses a given career track actually evaluates (RF-03.02)
CREATE TABLE exam_type_course (
    exam_type_id BIGINT NOT NULL REFERENCES exam_type(id) ON DELETE CASCADE,
    course_id    BIGINT NOT NULL REFERENCES course(id) ON DELETE CASCADE,
    PRIMARY KEY (exam_type_id, course_id)
);

-- What a student is actually applying for (Letras, Ciencias, Arquitectura,
-- Biomédica, Derecho, Artes) — distinct from `area` (exam content areas
-- like Lectura/Matemáticas). Several careers share the same exam_type
-- (e.g. Biomédica and Ciencias both sit the "Ciencias" exam); the old
-- system's `tarea.main` boolean gestured at this split but never recorded
-- which exam_type a non-main career actually mapped to (that lived in
-- application code instead) — here the mapping is an explicit FK.
CREATE TABLE career (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    abbreviation    VARCHAR(20),
    exam_type_id    BIGINT NOT NULL REFERENCES exam_type(id),
    active          BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX ix_career_exam_type ON career (exam_type_id);

-- ---------------------------------------------------------------------
-- Academic structure — Ciclo -> Etapa (RF-02.01/02)
-- ---------------------------------------------------------------------
CREATE TABLE cycle (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE,
    status      VARCHAR(20) NOT NULL DEFAULT 'PLANNED'
                CHECK (status IN ('PLANNED', 'ACTIVE', 'CLOSED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT REFERENCES app_user(id),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  BIGINT REFERENCES app_user(id)
);

CREATE TABLE stage (
    id          BIGSERIAL PRIMARY KEY,
    cycle_id    BIGINT NOT NULL REFERENCES cycle(id),
    name        VARCHAR(150) NOT NULL,
    sequence    INT NOT NULL,
    start_date  DATE,
    end_date    DATE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT REFERENCES app_user(id),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  BIGINT REFERENCES app_user(id),
    UNIQUE (cycle_id, sequence)
);
CREATE INDEX ix_stage_cycle ON stage (cycle_id);

-- ---------------------------------------------------------------------
-- People
-- ---------------------------------------------------------------------
CREATE TABLE student (
    id              BIGSERIAL PRIMARY KEY,
    first_name      VARCHAR(150) NOT NULL,
    last_name_pat   VARCHAR(100) NOT NULL,
    last_name_mat   VARCHAR(100),
    dni             VARCHAR(20),
    email           VARCHAR(255),
    phone           VARCHAR(30),
    birth_date      DATE,
    active          BOOLEAN NOT NULL DEFAULT TRUE, -- soft-delete: retired students are never hard-deleted
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT REFERENCES app_user(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT REFERENCES app_user(id)
);
-- DNI is optional (many students don't have one registered yet) but must
-- be unique whenever it IS present — a partial index is how Postgres
-- expresses "unique, ignoring NULLs" (RF-04.04 support).
CREATE UNIQUE INDEX ux_student_dni ON student (dni) WHERE dni IS NOT NULL;
CREATE INDEX ix_student_names ON student (last_name_pat, last_name_mat, first_name);

CREATE TABLE parent (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(200) NOT NULL,
    phone       VARCHAR(30) NOT NULL,
    email       VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT REFERENCES app_user(id),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  BIGINT REFERENCES app_user(id)
);

-- relationship_type lives on the link, not on `parent`: the same person
-- could in principle be "Hermano/a" to one student and "Otro" to another,
-- so the label only makes sense relative to a specific student.
CREATE TABLE student_parent (
    student_id          BIGINT NOT NULL REFERENCES student(id) ON DELETE CASCADE,
    parent_id           BIGINT NOT NULL REFERENCES parent(id) ON DELETE CASCADE,
    relationship_type    VARCHAR(20) NOT NULL
                         CHECK (relationship_type IN ('MADRE', 'PADRE', 'HERMANO', 'OTRO')),
    PRIMARY KEY (student_id, parent_id)
);

-- ---------------------------------------------------------------------
-- Enrollment & tutoring — one row per student per stage (RF-02.04),
-- so a repeating student's prior-cycle data is naturally partitioned
-- off from the current cycle's averages (US-07) without extra flags.
-- ---------------------------------------------------------------------
CREATE TABLE student_stage (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT NOT NULL REFERENCES student(id),
    stage_id        BIGINT NOT NULL REFERENCES stage(id),
    career_id       BIGINT REFERENCES career(id), -- what the student is applying for during this stage; nullable (a student can enroll before deciding), mutable — can change mid-stage. The exam_type of any given result stays pinned via exam.exam_type_id, so changing this never rewrites past comparisons.
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT REFERENCES app_user(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT REFERENCES app_user(id),
    UNIQUE (student_id, stage_id)
);
CREATE INDEX ix_student_stage_student ON student_stage (student_id);
CREATE INDEX ix_student_stage_stage ON student_stage (stage_id);

-- Replaces the old mutable "UPDATE tutor_id" with an append-only history:
-- reassigning a tutor closes the current row (sets valid_to) and inserts
-- a new one, so "who was tutoring this student on date X" is a query,
-- not a lost fact (RF-02.05).
CREATE TABLE tutor_assignment (
    id                  BIGSERIAL PRIMARY KEY,
    student_stage_id    BIGINT NOT NULL REFERENCES student_stage(id),
    tutor_id            BIGINT NOT NULL REFERENCES app_user(id),
    valid_from          TIMESTAMPTZ NOT NULL DEFAULT now(),
    valid_to            TIMESTAMPTZ, -- NULL = this is the current tutor
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          BIGINT REFERENCES app_user(id)
);
CREATE INDEX ix_tutor_assignment_student_stage ON tutor_assignment (student_stage_id);
-- at most one *open* assignment (valid_to IS NULL) per student_stage
CREATE UNIQUE INDEX ux_tutor_assignment_current
    ON tutor_assignment (student_stage_id) WHERE valid_to IS NULL;

-- ---------------------------------------------------------------------
-- Exams & results
-- ---------------------------------------------------------------------
CREATE TABLE exam (
    id              BIGSERIAL PRIMARY KEY,
    stage_id        BIGINT NOT NULL REFERENCES stage(id),
    exam_type_id    BIGINT NOT NULL REFERENCES exam_type(id),
    name            VARCHAR(200) NOT NULL,
    exam_date       DATE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT REFERENCES app_user(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT REFERENCES app_user(id)
);
CREATE INDEX ix_exam_stage ON exam (stage_id);

CREATE TABLE exam_result (
    id                  BIGSERIAL PRIMARY KEY,
    exam_id             BIGINT NOT NULL REFERENCES exam(id),
    student_stage_id    BIGINT NOT NULL REFERENCES student_stage(id),
    total_correct       INT,
    total_wrong         INT,
    total_blank         INT,
    total_score         NUMERIC(6,2),
    -- false when the import detected the "didn't take the exam" pattern
    -- (score stuck at the 80 floor, every course fully blank) — the row
    -- is kept, never discarded, just excluded from averages/comparisons.
    attended            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          BIGINT REFERENCES app_user(id),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          BIGINT REFERENCES app_user(id),
    -- one result per student per exam, enforced by the database itself —
    -- re-importing the same exam can no longer silently overwrite (RF-04.03).
    -- A forced admin reload must delete-then-reinsert in one transaction.
    UNIQUE (exam_id, student_stage_id)
);
CREATE INDEX ix_exam_result_student_stage ON exam_result (student_stage_id);

CREATE TABLE exam_course_result (
    id              BIGSERIAL PRIMARY KEY,
    exam_result_id  BIGINT NOT NULL REFERENCES exam_result(id) ON DELETE CASCADE,
    course_id       BIGINT NOT NULL REFERENCES course(id),
    correct         INT NOT NULL DEFAULT 0,
    wrong           INT NOT NULL DEFAULT 0,
    blank           INT NOT NULL DEFAULT 0,
    score           NUMERIC(6,2),
    UNIQUE (exam_result_id, course_id)
);

-- ---------------------------------------------------------------------
-- Goals — per area (Lectura / Matemáticas), tied to the catalog instead
-- of hardcoded lectGoal/mateGoal columns (RF-06.01).
-- ---------------------------------------------------------------------
CREATE TABLE goal (
    id                  BIGSERIAL PRIMARY KEY,
    student_stage_id    BIGINT NOT NULL REFERENCES student_stage(id),
    area_id             BIGINT NOT NULL REFERENCES area(id),
    target_exam_id      BIGINT NOT NULL REFERENCES exam(id), -- the exam this goal is aiming at
    target_correct      INT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          BIGINT REFERENCES app_user(id),
    UNIQUE (student_stage_id, target_exam_id, area_id)
);

-- ---------------------------------------------------------------------
-- Import staging — nothing from an Excel upload becomes a real Student
-- or ExamResult until a human resolves every ambiguous row (RF-04.04).
-- ---------------------------------------------------------------------
CREATE TABLE import_batch (
    id              BIGSERIAL PRIMARY KEY,
    type            VARCHAR(20) NOT NULL CHECK (type IN ('STUDENTS', 'EXAM_RESULTS')),
    exam_id         BIGINT REFERENCES exam(id), -- only set for EXAM_RESULTS batches
    file_name       VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW'
                    CHECK (status IN ('PENDING_REVIEW', 'CONFIRMED', 'DISCARDED', 'SUPERSEDED')),
    uploaded_by     BIGINT NOT NULL REFERENCES app_user(id),
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_by    BIGINT REFERENCES app_user(id),
    confirmed_at    TIMESTAMPTZ
);
CREATE INDEX ix_import_batch_exam ON import_batch (exam_id);

CREATE TABLE import_row (
    id                  BIGSERIAL PRIMARY KEY,
    batch_id            BIGINT NOT NULL REFERENCES import_batch(id) ON DELETE CASCADE,
    row_number          INT NOT NULL,
    raw_payload         JSONB NOT NULL,       -- the original spreadsheet row, parsed
    match_status        VARCHAR(20) NOT NULL DEFAULT 'UNRESOLVED'
                         CHECK (match_status IN ('AUTO_MATCHED', 'MANUAL_MATCHED', 'NEW_STUDENT', 'UNRESOLVED', 'IGNORED')),
    matched_student_id  BIGINT REFERENCES student(id),
    candidate_matches   JSONB,                -- fuzzy-match suggestions: [{studentId, score}, ...]
    resolved_by         BIGINT REFERENCES app_user(id),
    resolved_at         TIMESTAMPTZ
);
CREATE INDEX ix_import_row_batch ON import_row (batch_id);

-- ---------------------------------------------------------------------
-- Psychological logbook (bitácora) — RF-05.xx. Hangs off the student
-- directly, never off a student_stage, so it survives regrouping and
-- cycle changes. Entries are insert-only: the API never exposes an
-- update/delete, so there is intentionally no updated_at/by here.
-- ---------------------------------------------------------------------
CREATE TABLE log_entry (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT NOT NULL REFERENCES student(id),
    author_id       BIGINT NOT NULL REFERENCES app_user(id),
    motivo          TEXT,
    acuerdos        TEXT,
    observaciones   TEXT,
    extra           JSONB, -- future fields a tutor wants to track, without a migration
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_log_entry_student ON log_entry (student_id, created_at DESC);
