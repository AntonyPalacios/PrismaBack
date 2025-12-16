SET FOREIGN_KEY_CHECKS = 0;

-- 2. Limpiar la tabla COMPLETAMENTE
-- Usamos TRUNCATE en lugar de DELETE porque:
--    a) Es más rápido.
--    b) Resetea los IDs autoincrementales a 1 (Adiós al problema de IDs 33, 34).
TRUNCATE TABLE db_prisma.tcourse;

-- 3. Volver a activar los chequeos (Muy importante para que el test sea válido)
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO db_prisma.tcourse ( abbreviation, main, name, id_parent_course) VALUES ( 'LECT', true, 'Lectura', null);
INSERT INTO db_prisma.tcourse ( abbreviation, main, name, id_parent_course) VALUES ( 'MATE', true, 'Matemática', null);
INSERT INTO db_prisma.tcourse ( abbreviation, main, name, id_parent_course) VALUES ( 'NYO', false, 'Números y Operaciones', 2);
INSERT INTO db_prisma.tcourse ( abbreviation, main, name, id_parent_course) VALUES ( 'GEO', false, 'Geometría', 2);
INSERT INTO db_prisma.tcourse ( abbreviation, main, name, id_parent_course) VALUES ( 'X', false, 'Álgebra', 2);
INSERT INTO db_prisma.tcourse ( abbreviation, main, name, id_parent_course) VALUES ( 'TRIGO', false, 'Trigonometría', 2);
INSERT INTO db_prisma.tcourse ( abbreviation, main, name, id_parent_course) VALUES ( 'EST', false, 'Estadística', 2);
INSERT INTO db_prisma.tcourse ( abbreviation, main, name, id_parent_course) VALUES ( 'LECT', false, 'Lectura', 1);
