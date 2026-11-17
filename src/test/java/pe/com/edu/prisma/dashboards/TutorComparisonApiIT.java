package pe.com.edu.prisma.dashboards;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pe.com.edu.prisma.config.ContainersConfig;
import pe.com.edu.prisma.security.AuthenticatedUser;

/**
 * The tutor comparison (REQ-DASHBOARDS-06, agreements of 2026-11-17). The
 * active cycle spans today, with four Letras exams. Ana and Beto are Silvia's
 * (seed) students, Carla is Ruth's (invented) and Dario has no tutor: he
 * only counts for the academy. Every person is invented.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class TutorComparisonApiIT {

    private static final long LETRAS = 2;
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Lima"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long admin;
    private long silvia;
    private long ruth;
    private long exam2;
    private long exam3;
    private long exam4;

    @BeforeEach
    void seed() {
        emptyTables();
        admin = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'apalaciosc0109@gmail.com'", Long.class);
        silvia = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'silvia.valleg@prisma.edu.pe'", Long.class);
        ruth = jdbc.queryForObject("INSERT INTO app_user (email, first_name, last_name_pat) "
                + "VALUES ('ruth.comparativa@prueba.pe', 'Ruth', 'Salas') RETURNING id", Long.class);
        jdbc.update("INSERT INTO user_role (user_id, role_id) SELECT ?, id FROM role WHERE name = 'TUTOR'", ruth);
        long cycle = jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) VALUES ('Actual', ?, ?, 'ACTIVE') "
                + "RETURNING id", Long.class, TODAY.minusMonths(2), TODAY.plusMonths(2));
        long stage = jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date) VALUES (?, 'Regular', ?, ?) "
                + "RETURNING id", Long.class, cycle, TODAY.minusMonths(2), TODAY.plusMonths(2));
        long exam1 = exam(stage, "Regular 1", TODAY.minusDays(40));
        exam2 = exam(stage, "Regular 2", TODAY.minusDays(30));
        exam3 = exam(stage, "Regular 3", TODAY.minusDays(20));
        exam4 = exam(stage, "Regular 4", TODAY.minusDays(10));

        long ana = enroll("Ana", stage, silvia);
        long beto = enroll("Beto", stage, silvia);
        long carla = enroll("Carla", stage, ruth);
        long dario = enroll("Dario", stage, null);

        result(exam1, ana, true, 1, 1, 1, 1); // too old: only the last 3 exams count
        result(exam2, ana, true, 10, 2, 5, 1);
        result(exam2, beto, true, 6, 4, 3, 3);
        result(exam2, carla, true, 8, 0, 9, 1);
        result(exam2, dario, true, 4, 4, 2, 2);
        result(exam3, ana, true, 12, 0, 6, 0);
        result(exam3, beto, true, 0, 0, 0, 0); // a zero that counts
        result(exam3, carla, false, 0, 0, 0, 0); // did not attend: Ruth has no data in Regular 3
        result(exam4, carla, true, 14, 2, 8, 2);
    }

    @AfterEach
    void emptyTables() {
        jdbc.update("DELETE FROM exam_result");
        jdbc.update("DELETE FROM exam");
        jdbc.update("DELETE FROM mock_exam");
        jdbc.update("DELETE FROM tutor_assignment");
        jdbc.update("DELETE FROM student_stage");
        jdbc.update("DELETE FROM student");
        jdbc.update("DELETE FROM stage");
        jdbc.update("DELETE FROM cycle");
        jdbc.update("DELETE FROM user_role WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@prueba.pe')");
        jdbc.update("DELETE FROM app_user WHERE email LIKE '%@prueba.pe'");
    }

    private long exam(long stageId, String name, LocalDate date) {
        long mockExam = jdbc.queryForObject("INSERT INTO mock_exam (stage_id, name, exam_date) VALUES (?, ?, ?) RETURNING id",
                Long.class, stageId, name, date);
        return jdbc.queryForObject("INSERT INTO exam (mock_exam_id, exam_type_id) VALUES (?, ?) RETURNING id",
                Long.class, mockExam, LETRAS);
    }

    private long enroll(String firstName, long stageId, Long tutorId) {
        long student = jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat) VALUES (?, 'Prueba') RETURNING id",
                Long.class, firstName);
        long enrollment = jdbc.queryForObject("INSERT INTO student_stage (student_id, stage_id) VALUES (?, ?) RETURNING id",
                Long.class, student, stageId);
        if (tutorId != null) {
            jdbc.update("INSERT INTO tutor_assignment (student_stage_id, tutor_id) VALUES (?, ?)", enrollment, tutorId);
        }
        return enrollment;
    }

    /** Lectura (course 1) and Álgebra (course 3, Matemática): correct and wrong answers. */
    private void result(long examId, long enrollmentId, boolean attended, int readingCorrect, int readingWrong,
            int mathCorrect, int mathWrong) {
        long result = jdbc.queryForObject("INSERT INTO exam_result (exam_id, student_stage_id, total_score, attended) "
                + "VALUES (?, ?, 500, ?) RETURNING id", Long.class, examId, enrollmentId, attended);
        jdbc.update("INSERT INTO exam_course_result (exam_result_id, course_id, correct, wrong, blank) VALUES "
                + "(?, 1, ?, ?, 0), (?, 3, ?, ?, 0)", result, readingCorrect, readingWrong, result, mathCorrect, mathWrong);
    }

    private ResultActions comparison(String role, long userId, long examTypeId) throws Exception {
        AuthenticatedUser principal = () -> userId;
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return mockMvc.perform(get("/api/v1/dashboards/tutor-comparison").param("examTypeId", String.valueOf(examTypeId))
                .with(authentication(auth)));
    }

    @Test
    void eachTutorsGroupInTheLastThreeExamsAgainstTheAcademy() throws Exception {
        comparison("ADMIN", admin, LETRAS).andExpect(status().isOk())
                .andExpect(jsonPath("$.exams.length()").value(3))
                .andExpect(jsonPath("$.exams[0].examId").value(exam2))
                .andExpect(jsonPath("$.exams[2].examName").value("Regular 4"))
                // by name: Salas, Ruth before Valle Gómez, Silvia
                .andExpect(jsonPath("$.tutors.length()").value(2))
                .andExpect(jsonPath("$.tutors[0].tutorId").value(ruth))
                .andExpect(jsonPath("$.tutors[1].tutorName").value("Valle Gómez, Silvia"))
                // Silvia: Ana and Beto in Regular 2 and 3; nobody in Regular 4
                .andExpect(jsonPath("$.tutors[1].cells.length()").value(2))
                .andExpect(jsonPath("$.tutors[1].cells[0].examId").value(exam2))
                .andExpect(jsonPath("$.tutors[1].cells[0].students").value(2))
                .andExpect(jsonPath("$.tutors[1].cells[0].areas[0].areaId").value(1))
                .andExpect(jsonPath("$.tutors[1].cells[0].areas[0].correctAvg").value(8.0))
                .andExpect(jsonPath("$.tutors[1].cells[0].areas[0].wrongAvg").value(3.0))
                .andExpect(jsonPath("$.tutors[1].cells[0].areas[1].correctAvg").value(4.0))
                .andExpect(jsonPath("$.tutors[1].cells[1].examId").value(exam3))
                .andExpect(jsonPath("$.tutors[1].cells[1].areas[0].correctAvg").value(6.0))
                // Ruth: Carla did not attend Regular 3, so there is no cell for it (never a 0)
                .andExpect(jsonPath("$.tutors[0].cells.length()").value(2))
                .andExpect(jsonPath("$.tutors[0].cells[0].examId").value(exam2))
                .andExpect(jsonPath("$.tutors[0].cells[1].examId").value(exam4))
                // the academy counts Dario, who has no tutor
                .andExpect(jsonPath("$.academy.length()").value(3))
                .andExpect(jsonPath("$.academy[0].students").value(4))
                .andExpect(jsonPath("$.academy[0].areas[0].correctAvg").value(7.0))
                .andExpect(jsonPath("$.academy[0].areas[1].correctAvg").value(4.75))
                .andExpect(jsonPath("$.academy[1].students").value(2))
                .andExpect(jsonPath("$.academy[2].students").value(1));
    }

    @Test
    void anExamTypeWithoutResultsGivesNothingToCompare() throws Exception {
        comparison("ADMIN", admin, 1).andExpect(status().isOk())
                .andExpect(jsonPath("$.exams.length()").value(0))
                .andExpect(jsonPath("$.tutors.length()").value(0))
                .andExpect(jsonPath("$.academy.length()").value(0));
        comparison("ADMIN", admin, 999_999).andExpect(status().isNotFound());
    }

    @Test
    void onlyAnAdmin() throws Exception {
        comparison("TUTOR", silvia, LETRAS).andExpect(status().isForbidden());
        comparison("COORDINADOR", admin, LETRAS).andExpect(status().isForbidden());
    }
}
