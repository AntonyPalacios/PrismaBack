-- REQ-SECURITY-01: Google's profile photo, refreshed on every login and
-- shown in the app layout. Nullable: users who never logged in have none.
ALTER TABLE app_user ADD COLUMN photo_url VARCHAR(1024);

-- Day-one users. Registration is admin-managed (Google login never creates
-- users), so without these rows nobody could ever get in.
--
-- Antony is the permanent ADMIN (project agreement, immutable): this row is
-- the root of trust for every other user created from the admin UI.
INSERT INTO app_user (email, first_name, last_name_pat, last_name_mat)
VALUES ('apalaciosc0109@gmail.com', 'Antony', 'Palacios', 'Coronado'),
       ('silvia.valleg@prisma.edu.pe', 'Silvia', 'Valle', 'Gómez');

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM app_user u JOIN role r ON r.name = 'ADMIN'
WHERE u.email = 'apalaciosc0109@gmail.com';

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM app_user u JOIN role r ON r.name = 'TUTOR'
WHERE u.email = 'silvia.valleg@prisma.edu.pe';
