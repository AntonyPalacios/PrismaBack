package pe.com.edu.prisma.students;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pe.com.edu.prisma.config.ContainersConfig;

/**
 * Mass enrollment (REQ-STUDENTS-05, agreements of 2026-11-17). The active
 * cycle has "Etapa 1" (the source) and "Etapa 2" (the target); a closed
 * cycle has one more stage. Etapa 1: Ana (Letras, Silvia), Beto (no career,
 * Ruth, who was deactivated), Carla (already in Etapa 2), Dario (deactivated)
 * and Elena (withdrawn: not listed). Every person is invented.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class BulkEnrollmentApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long silvia;
    private long ruth;
    private long letras;
    private long ciencias;
    private long source;
    private long target;
    private long closedStage;
    private long ana;
    private long beto;
    private long carla;
    private long dario;

    @BeforeEach
    void seed() {
        emptyTables();
        silvia = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'silvia.valleg@prisma.edu.pe'", Long.class);
        ruth = jdbc.queryForObject("INSERT INTO app_user (email, first_name, last_name_pat) "
                + "VALUES ('ruth.lote@prueba.pe', 'Ruth', 'Salas') RETURNING id", Long.class);
        jdbc.update("INSERT INTO user_role (user_id, role_id) SELECT ?, id FROM role WHERE name = 'TUTOR'", ruth);
        letras = jdbc.queryForObject("SELECT id FROM career WHERE name = 'Letras'", Long.class);
        ciencias = jdbc.queryForObject("SELECT id FROM career WHERE name = 'Ciencias'", Long.class);

        long cycle = jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) "
                + "VALUES ('2026-I', '2026-01-01', '2026-06-30', 'ACTIVE') RETURNING id", Long.class);
        source = stage(cycle, "Etapa 1", "2026-01-01", "2026-02-28");
        target = stage(cycle, "Etapa 2", "2026-03-01", "2026-06-30");
        long closedCycle = jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) "
                + "VALUES ('2025-I', '2025-01-01', '2025-06-30', 'CLOSED') RETURNING id", Long.class);
        closedStage = stage(closedCycle, "Etapa 1", "2025-01-01", "2025-06-30");

        ana = student("Ana", "Ramos", true);
        beto = student("Beto", "Pinto", true);
        carla = student("Carla", "Quiroz", true);
        dario = student("Dario", "Torres", false);
        long elena = student("Elena", "Arias", true);
        enroll(ana, source, letras, silvia, true);
        enroll(beto, source, null, ruth, true);
        enroll(carla, source, ciencias, null, true);
        enroll(carla, target, null, null, true);
        enroll(dario, source, null, null, true);
        enroll(elena, source, null, null, false);
        // Ruth leaves after tutoring Beto: her assignment stays, but she is no longer pickable
        jdbc.update("UPDATE app_user SET active = false WHERE id = ?", ruth);
    }

    @AfterEach
    void emptyTables() {
        jdbc.update("DELETE FROM tutor_assignment");
        jdbc.update("DELETE FROM student_stage");
        jdbc.update("DELETE FROM student");
        jdbc.update("DELETE FROM stage");
        jdbc.update("DELETE FROM cycle");
        jdbc.update("DELETE FROM career WHERE name LIKE 'Prueba %'");
        jdbc.update("DELETE FROM user_role WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@prueba.pe')");
        jdbc.update("DELETE FROM app_user WHERE email LIKE '%@prueba.pe'");
    }

    private long stage(long cycle, String name, String start, String end) {
        return jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date) "
                + "VALUES (?, ?, ?::date, ?::date) RETURNING id", Long.class, cycle, name, start, end);
    }

    private long student(String firstName, String lastNamePat, boolean active) {
        return jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat, active) VALUES (?, ?, ?) RETURNING id",
                Long.class, firstName, lastNamePat, active);
    }

    private void enroll(long studentId, long stageId, Long careerId, Long tutorId, boolean active) {
        long enrollment = jdbc.queryForObject("INSERT INTO student_stage (student_id, stage_id, career_id, active) "
                + "VALUES (?, ?, ?, ?) RETURNING id", Long.class, studentId, stageId, careerId, active);
        if (tutorId != null) {
            jdbc.update("INSERT INTO tutor_assignment (student_stage_id, tutor_id) VALUES (?, ?)", enrollment, tutorId);
        }
    }

    private ResultActions preview(String role, Object sourceId, Object targetId) throws Exception {
        return mockMvc.perform(post("/api/v1/enrollments/bulk-preview").with(user("someone").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceStageId\":" + sourceId + ",\"targetStageId\":" + targetId + "}"));
    }

    private ResultActions bulk(String role, Object targetId, String rows) throws Exception {
        return mockMvc.perform(post("/api/v1/enrollments/bulk").with(user("someone").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetStageId\":" + targetId + ",\"rows\":[" + rows + "]}"));
    }

    private static String row(long studentId, Long careerId, Long tutorId) {
        return "{\"studentId\":" + studentId + ",\"careerId\":" + careerId + ",\"tutorUserId\":" + tutorId + "}";
    }

    private Integer count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    void previewListsTheSourceStudentsWithTheirCareerAndTutor() throws Exception {
        preview("ADMIN", source, target).andExpect(status().isOk())
                // by last name; Elena withdrew from the source stage
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].fullName").value("Pinto, Beto"))
                // Ruth was deactivated: no tutor to preload
                .andExpect(jsonPath("$[0].tutorUserId").isEmpty())
                .andExpect(jsonPath("$[0].careerId").isEmpty())
                .andExpect(jsonPath("$[0].skipReason").isEmpty())
                .andExpect(jsonPath("$[1].studentId").value(carla))
                .andExpect(jsonPath("$[1].careerId").value(ciencias))
                .andExpect(jsonPath("$[1].skipReason").value("ALREADY_ENROLLED"))
                .andExpect(jsonPath("$[2].studentId").value(ana))
                .andExpect(jsonPath("$[2].careerId").value(letras))
                .andExpect(jsonPath("$[2].tutorUserId").value(silvia))
                .andExpect(jsonPath("$[3].studentId").value(dario))
                .andExpect(jsonPath("$[3].skipReason").value("INACTIVE_STUDENT"));
    }

    @Test
    void anInactiveCareerIsNotPreloaded() throws Exception {
        long inactive = jdbc.queryForObject("INSERT INTO career (name, exam_type_id, active) "
                + "VALUES ('Prueba inactiva', 1, false) RETURNING id", Long.class);
        jdbc.update("UPDATE student_stage SET career_id = ? WHERE student_id = ? AND stage_id = ?", inactive, ana, source);
        preview("ADMIN", source, target).andExpect(status().isOk())
                .andExpect(jsonPath("$[2].studentId").value(ana))
                .andExpect(jsonPath("$[2].careerId").isEmpty());
    }

    @Test
    void enrollsTheBatchWithCareerAndTutorAndSkipsTheKnownOnes() throws Exception {
        bulk("ADMIN", target, String.join(",", row(ana, letras, silvia), row(beto, ciencias, null),
                row(carla, null, null), row(dario, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.skipped").value(2));

        assertThat(count("SELECT count(*) FROM student_stage WHERE stage_id = ?", target)).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM student_stage WHERE stage_id = ? AND student_id = ? AND career_id = ?",
                target, beto, ciencias)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM tutor_assignment a JOIN student_stage e ON e.id = a.student_stage_id "
                + "WHERE e.stage_id = ? AND e.student_id = ? AND a.tutor_id = ? AND a.valid_to IS NULL",
                target, ana, silvia)).isEqualTo(1);
        // Carla keeps her single enrollment in the target stage; Dario is not enrolled
        assertThat(count("SELECT count(*) FROM student_stage WHERE stage_id = ? AND student_id = ?", target, carla))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM student_stage WHERE stage_id = ? AND student_id = ?", target, dario))
                .isZero();
    }

    @Test
    void anyRejectedRowWritesNothing() throws Exception {
        // Ruth is no longer an active tutor: Ana's row, already processed, is undone too
        bulk("ADMIN", target, String.join(",", row(ana, letras, silvia), row(beto, null, ruth)))
                .andExpect(status().isConflict());
        long inactive = jdbc.queryForObject("INSERT INTO career (name, exam_type_id, active) "
                + "VALUES ('Prueba inactiva', 1, false) RETURNING id", Long.class);
        bulk("ADMIN", target, String.join(",", row(ana, letras, null), row(beto, inactive, null)))
                .andExpect(status().isConflict());
        assertThat(count("SELECT count(*) FROM student_stage WHERE stage_id = ?", target)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM tutor_assignment")).isEqualTo(2);
    }

    @Test
    void rejectsWrongBatches() throws Exception {
        preview("ADMIN", source, source).andExpect(status().isBadRequest());
        preview("ADMIN", source, closedStage).andExpect(status().isConflict());
        preview("ADMIN", 999_999, target).andExpect(status().isNotFound());
        bulk("ADMIN", target, "").andExpect(status().isBadRequest());
        bulk("ADMIN", target, String.join(",", row(ana, null, null), row(ana, null, null)))
                .andExpect(status().isBadRequest());
        bulk("ADMIN", closedStage, row(ana, null, null)).andExpect(status().isConflict());
        bulk("ADMIN", target, row(999_999, null, null)).andExpect(status().isNotFound());
        assertThat(count("SELECT count(*) FROM student_stage WHERE stage_id = ?", target)).isEqualTo(1);
    }

    @Test
    void onlyAnAdmin() throws Exception {
        preview("TUTOR", source, target).andExpect(status().isForbidden());
        preview("COORDINADOR", source, target).andExpect(status().isForbidden());
        bulk("TUTOR", target, row(ana, null, null)).andExpect(status().isForbidden());
        bulk("COORDINADOR", target, row(ana, null, null)).andExpect(status().isForbidden());
    }
}
