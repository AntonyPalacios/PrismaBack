package pe.com.edu.prisma.students;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.com.edu.prisma.config.ContainersConfig;
import pe.com.edu.prisma.security.AuthenticatedUser;

/**
 * Merging duplicate students (agreements of 2026-11-18). "Keep" and "Remove"
 * are the same invented student registered twice. Both are enrolled in
 * Regular (shared stage); only Remove is enrolled in Repaso. Regular 1 has a
 * Letras and a Ciencias exam, Regular 2 a Letras one; Repaso 1 a Letras one.
 * Lectura is area 1, Matemática area 2.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class StudentMergeApiIT {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Lima"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long admin;
    private long silvia;
    private long ruth;
    private long keep;
    private long remove;
    private long regular;
    private long repaso;
    private long r1Letras;
    private long r1Ciencias;
    private long r2Letras;
    private long repasoLetras;
    private long keepRegular;
    private long removeRegular;
    private long removeRepaso;
    private long parent;

    @BeforeEach
    void seed() {
        emptyTables();
        admin = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'apalaciosc0109@gmail.com'", Long.class);
        silvia = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'silvia.valleg@prisma.edu.pe'", Long.class);
        ruth = jdbc.queryForObject("INSERT INTO app_user (email, first_name, last_name_pat) "
                + "VALUES ('ruth.fusion@prueba.pe', 'Ruth', 'Salas') RETURNING id", Long.class);
        jdbc.update("INSERT INTO user_role (user_id, role_id) SELECT ?, id FROM role WHERE name = 'TUTOR'", ruth);
        long letras = jdbc.queryForObject("SELECT id FROM exam_type WHERE name = 'Letras'", Long.class);
        long ciencias = jdbc.queryForObject("SELECT id FROM exam_type WHERE name = 'Ciencias'", Long.class);
        long letrasCareer = jdbc.queryForObject("SELECT id FROM career WHERE name = 'Letras'", Long.class);

        long cycle = jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) VALUES ('Actual', ?, ?, 'ACTIVE') "
                + "RETURNING id", Long.class, TODAY.minusMonths(3), TODAY.plusMonths(3));
        regular = stage(cycle, "Regular", TODAY.minusMonths(3), TODAY.minusMonths(1));
        repaso = stage(cycle, "Repaso", TODAY.minusMonths(1).plusDays(1), TODAY.plusMonths(3));
        long r1 = mockExam(regular, "Regular 1");
        r1Letras = exam(r1, letras);
        r1Ciencias = exam(r1, ciencias);
        r2Letras = exam(mockExam(regular, "Regular 2"), letras);
        repasoLetras = exam(mockExam(repaso, "Repaso 1"), letras);

        keep = student("Keep", null, null);
        remove = student("Remove", "61841640", "remove@prueba.pe");
        keepRegular = enroll(keep, regular, null, silvia);
        removeRegular = enroll(remove, regular, letrasCareer, ruth);
        removeRepaso = enroll(remove, repaso, null, null);

        result(keepRegular, r1Ciencias, false, 80);   // did not attend Ciencias...
        result(removeRegular, r1Letras, true, 600);   // ...but sat Letras of the same mock exam
        result(keepRegular, r2Letras, true, 500);
        result(removeRegular, r2Letras, true, 450);   // the same exam: the kept 500 is better
        result(removeRepaso, repasoLetras, true, 700);
        goal(keepRegular, r2Letras, 1, 10);
        goal(removeRegular, r2Letras, 1, 12);         // collides: the kept goal stays
        goal(removeRegular, r2Letras, 2, 7);          // moves

        jdbc.update("INSERT INTO log_entry (student_id, author_id, motivo, session_date, meeting_with) "
                + "VALUES (?, ?, 'Seguimiento', ?, 'STUDENT')", remove, ruth, TODAY);
        parent = jdbc.queryForObject("INSERT INTO parent (full_name, phone) VALUES ('Rosa Fusión', '999111222') RETURNING id",
                Long.class);
        jdbc.update("INSERT INTO student_parent (student_id, parent_id, relationship_type) VALUES (?, ?, 'MADRE')", remove, parent);
    }

    @AfterEach
    void emptyTables() {
        jdbc.update("DELETE FROM goal");
        jdbc.update("DELETE FROM exam_result");
        jdbc.update("DELETE FROM exam");
        jdbc.update("DELETE FROM mock_exam");
        jdbc.update("DELETE FROM log_entry");
        jdbc.update("DELETE FROM student_parent");
        jdbc.update("DELETE FROM parent");
        jdbc.update("DELETE FROM tutor_assignment");
        jdbc.update("DELETE FROM student_stage");
        jdbc.update("DELETE FROM student");
        jdbc.update("DELETE FROM stage");
        jdbc.update("DELETE FROM cycle");
        jdbc.update("DELETE FROM user_role WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@prueba.pe')");
        jdbc.update("DELETE FROM app_user WHERE email LIKE '%@prueba.pe'");
    }

    private long stage(long cycle, String name, LocalDate start, LocalDate end) {
        return jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, cycle, name, start, end);
    }

    private long mockExam(long stageId, String name) {
        return jdbc.queryForObject("INSERT INTO mock_exam (stage_id, name, exam_date) "
                + "SELECT ?, ?, start_date FROM stage WHERE id = ? RETURNING id", Long.class, stageId, name, stageId);
    }

    private long exam(long mockExamId, long typeId) {
        return jdbc.queryForObject("INSERT INTO exam (mock_exam_id, exam_type_id) VALUES (?, ?) RETURNING id",
                Long.class, mockExamId, typeId);
    }

    private long student(String firstName, String dni, String email) {
        return jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat, last_name_mat, dni, email) "
                + "VALUES (?, 'Vasquez', 'Villamonte', ?, ?) RETURNING id", Long.class, firstName, dni, email);
    }

    private long enroll(long studentId, long stageId, Long careerId, Long tutorId) {
        long enrollment = jdbc.queryForObject("INSERT INTO student_stage (student_id, stage_id, career_id) VALUES (?, ?, ?) "
                + "RETURNING id", Long.class, studentId, stageId, careerId);
        if (tutorId != null) {
            jdbc.update("INSERT INTO tutor_assignment (student_stage_id, tutor_id) VALUES (?, ?)", enrollment, tutorId);
        }
        return enrollment;
    }

    private void result(long enrollmentId, long examId, boolean attended, int score) {
        jdbc.update("INSERT INTO exam_result (exam_id, student_stage_id, total_score, attended) VALUES (?, ?, ?, ?)",
                examId, enrollmentId, score, attended);
    }

    private void goal(long enrollmentId, long examId, long areaId, int target) {
        jdbc.update("INSERT INTO goal (student_stage_id, area_id, target_exam_id, target_correct) VALUES (?, ?, ?, ?)",
                enrollmentId, areaId, examId, target);
    }

    private ResultActions as(String role, MockHttpServletRequestBuilder request) throws Exception {
        AuthenticatedUser principal = () -> admin;
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return mockMvc.perform(request.with(authentication(auth)).with(csrf()).contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions merge(String role, long keepId, long removeId) throws Exception {
        return as(role, post("/api/v1/students/" + keepId + "/merge").content("{\"removeId\":" + removeId + "}"));
    }

    private Integer count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    void thePreviewShowsBothAndWhatCollides() throws Exception {
        as("ADMIN", get("/api/v1/students/" + keep + "/merge-preview").param("removeId", String.valueOf(remove)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keep.fullName").value("Vasquez Villamonte, Keep"))
                .andExpect(jsonPath("$.keep.enrollments").value(1))
                .andExpect(jsonPath("$.keep.results").value(2))
                .andExpect(jsonPath("$.remove.enrollments").value(2))
                .andExpect(jsonPath("$.remove.results").value(3))
                .andExpect(jsonPath("$.remove.goals").value(2))
                .andExpect(jsonPath("$.remove.logEntries").value(1))
                .andExpect(jsonPath("$.remove.parents").value(1))
                .andExpect(jsonPath("$.sharedStages.length()").value(1))
                .andExpect(jsonPath("$.sharedStages[0].stageLabel").value("Actual · Regular"))
                .andExpect(jsonPath("$.sharedStages[0].keepTutorName").value("Valle Gómez, Silvia"))
                .andExpect(jsonPath("$.sharedStages[0].removeTutorName").value("Salas, Ruth"))
                .andExpect(jsonPath("$.resultConflicts").value(1))
                .andExpect(jsonPath("$.goalConflicts").value(1))
                // the kept student has no DNI nor email: they come from the other
                .andExpect(jsonPath("$.merged.dni").value("61841640"))
                .andExpect(jsonPath("$.merged.email").value("remove@prueba.pe"));
    }

    @Test
    void everythingMovesToTheKeptStudentAndTheOtherIsDeleted(CapturedOutput output) throws Exception {
        merge("ADMIN", keep, remove).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(keep))
                .andExpect(jsonPath("$.firstName").value("Keep"))
                .andExpect(jsonPath("$.dni").value("61841640"))
                .andExpect(jsonPath("$.email").value("remove@prueba.pe"));

        assertThat(count("SELECT count(*) FROM student WHERE id = ?", remove)).isZero();
        // Repaso moved as is; Regular kept the kept student's enrollment, with the other's career
        assertThat(count("SELECT count(*) FROM student_stage WHERE student_id = ?", keep)).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM student_stage WHERE id = ? AND student_id = ?", removeRepaso, keep)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM student_stage WHERE id = ? AND career_id IS NOT NULL", keepRegular)).isEqualTo(1);
        // the kept tutor stays, the duplicate's is dropped
        assertThat(jdbc.queryForList("SELECT tutor_id FROM tutor_assignment WHERE student_stage_id = ?", Long.class, keepRegular))
                .containsExactly(silvia);
        assertThat(count("SELECT count(*) FROM tutor_assignment WHERE tutor_id = ?", ruth)).isZero();
        // Regular 1: Letras (sat) beats Ciencias (did not attend); Regular 2: the kept 500 beats 450
        assertThat(count("SELECT count(*) FROM exam_result WHERE exam_id = ?", r1Ciencias)).isZero();
        assertThat(jdbc.queryForObject("SELECT total_score FROM exam_result WHERE exam_id = ? AND student_stage_id = ?",
                BigDecimal.class, r1Letras, keepRegular)).isEqualByComparingTo("600");
        assertThat(jdbc.queryForObject("SELECT total_score FROM exam_result WHERE exam_id = ?", BigDecimal.class, r2Letras))
                .isEqualByComparingTo("500");
        assertThat(count("SELECT count(*) FROM exam_result WHERE exam_id = ? AND student_stage_id = ?", repasoLetras, removeRepaso))
                .isEqualTo(1);
        // the kept goal stays; the one that did not collide moves
        assertThat(jdbc.queryForList("SELECT target_correct FROM goal WHERE student_stage_id = ? ORDER BY area_id",
                Integer.class, keepRegular)).containsExactly(10, 7);
        assertThat(count("SELECT count(*) FROM log_entry WHERE student_id = ?", keep)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM student_parent WHERE student_id = ? AND parent_id = ?", keep, parent)).isEqualTo(1);
        assertThat(output).contains("Students merged: keptId=" + keep + " removedId=" + remove).doesNotContain("61841640");
    }

    @Test
    void onlyAnAdminMergesTwoDifferentStudents() throws Exception {
        merge("TUTOR", keep, remove).andExpect(status().isForbidden());
        merge("COORDINADOR", keep, remove).andExpect(status().isForbidden());
        as("TUTOR", get("/api/v1/students/" + keep + "/merge-preview").param("removeId", String.valueOf(remove)))
                .andExpect(status().isForbidden());
        merge("ADMIN", keep, keep).andExpect(status().isBadRequest());
        merge("ADMIN", keep, 999_999).andExpect(status().isNotFound());
        assertThat(count("SELECT count(*) FROM student WHERE id IN (?, ?)", keep, remove)).isEqualTo(2);
    }
}
