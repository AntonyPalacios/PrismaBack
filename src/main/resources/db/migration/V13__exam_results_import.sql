-- Importing exam results (agreements of 2026-11-16). Besides the total, the
-- academy's Excel brings each area's score (Lectura, Matemática), taken from
-- its conversion table: it is stored as it comes and never recalculated.
CREATE TABLE exam_area_result (
    id              BIGSERIAL PRIMARY KEY,
    exam_result_id  BIGINT NOT NULL REFERENCES exam_result(id) ON DELETE CASCADE,
    area_id         BIGINT NOT NULL REFERENCES area(id),
    score           NUMERIC(6,2) NOT NULL,
    UNIQUE (exam_result_id, area_id)
);

-- Rows of a results file that are not students (no name, leftovers of
-- sorting the sheet) are skipped when uploading, but counted in the batch.
ALTER TABLE import_batch ADD COLUMN skipped_rows INT NOT NULL DEFAULT 0;
