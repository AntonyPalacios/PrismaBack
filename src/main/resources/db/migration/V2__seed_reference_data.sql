-- Reference data that isn't "business logic" so much as fixed vocabulary
-- the academy already uses. All of it is editable later from the admin
-- UI (that's why it's rows, not enums) — this just seeds day one.

INSERT INTO role (name) VALUES ('ADMIN'), ('TUTOR'), ('COORDINADOR');

INSERT INTO area (id, name, abbreviation) VALUES
    (1, 'Lectura', 'LECT'),
    (2, 'Matemáticas', 'MATE');

INSERT INTO course (id, area_id, name, abbreviation) VALUES
    (1, 1, 'Lectura', 'LECT'),
    (2, 2, 'Números y Operaciones', 'NYO'),
    (3, 2, 'Álgebra', 'ALG'),
    (4, 2, 'Geometría', 'GEO'),
    (5, 2, 'Trigonometría', 'TRIGO'),
    (6, 2, 'Estadística', 'EST');

INSERT INTO exam_type (id, name) VALUES
    (1, 'Ciencias'),
    (2, 'Letras'),
    (3, 'Arquitectura');

-- Ciencias: todos los cursos menos Estadística
INSERT INTO exam_type_course (exam_type_id, course_id)
    SELECT 1, id FROM course WHERE id <> 6;

-- Letras: todos los cursos
INSERT INTO exam_type_course (exam_type_id, course_id)
    SELECT 2, id FROM course;

-- Arquitectura: todos menos Trigonometría y Estadística
INSERT INTO exam_type_course (exam_type_id, course_id)
    SELECT 3, id FROM course WHERE id NOT IN (5, 6);

-- Careers (a qué postula el alumno) and which exam_type each one sits.
-- Biomédica shares the Ciencias exam; Derecho and Artes share the Letras
-- exam — same split the old `tarea.main` flag pointed at, but explicit
-- here instead of hardcoded in application code.
INSERT INTO career (id, name, abbreviation, exam_type_id) VALUES
    (1, 'Ciencias',     'CC', 1),
    (2, 'Letras',       'LL', 2),
    (3, 'Arquitectura', 'Q',  3),
    (4, 'Biomédica',    'B',  1),
    (5, 'Derecho',      'D',  2),
    (6, 'Artes',        'A',  2);

-- keep the sequences in sync with the explicit ids inserted above
SELECT setval('area_id_seq', (SELECT MAX(id) FROM area));
SELECT setval('course_id_seq', (SELECT MAX(id) FROM course));
SELECT setval('exam_type_id_seq', (SELECT MAX(id) FROM exam_type));
SELECT setval('career_id_seq', (SELECT MAX(id) FROM career));
