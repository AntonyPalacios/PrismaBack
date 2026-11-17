package pe.com.edu.prisma.goals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Goals (REQ-GOALS-01, agreements of 2026-11-17). The active cycle spans
 * today, with Regular 1 and 2 already sat and Regular 3 ahead, each with a
 * Ciencias and a Letras exam. Silvia (seed) tutors Ana (Letras career), Beto
 * (no career, sat Ciencias) and Dario (Arquitectura career, a type Regular 3
 * lacks); Ruth (invented) tutors Carla, who never sat an exam. Every person
 * is invented. Lectura is course 1 (area 1) and Álgebra course 3 (area 2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class GoalApiIT {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Lima"));
    private static final long LECTURA = 1;
    private static final long MATEMATICA = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long admin;
    private long silvia;
    private long ruth;
    private long ciencias;
    private long letras;
    private long r1;
    private long r3;
    private long r1Letras;
    private long r3Letras;
    private long r3Ciencias;
    private long ana;
    private long anaStudent;
    private long beto;
    private long carla;
    private long dario;
    private long closedMockExam;

    @BeforeEach
    void seed() {
        emptyTables();
        admin = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'apalaciosc0109@gmail.com'", Long.class);
        silvia = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'silvia.valleg@prisma.edu.pe'", Long.class);
        ruth = jdbc.queryForObject("INSERT INTO app_user (email, first_name, last_name_pat) "
                + "VALUES ('ruth.metas@prueba.pe', 'Ruth', 'Salas') RETURNING id", Long.class);
        jdbc.update("INSERT INTO user_role (user_id, role_id) SELECT ?, id FROM role WHERE name = 'TUTOR'", ruth);
        ciencias = jdbc.queryForObject("SELECT id FROM exam_type WHERE name = 'Ciencias'", Long.class);
        letras = jdbc.queryForObject("SELECT id FROM exam_type WHERE name = 'Letras'", Long.class);

        long cycle = jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) VALUES ('Actual', ?, ?, 'ACTIVE') "
                + "RETURNING id", Long.class, TODAY.minusMonths(2), TODAY.plusMonths(2));
        long stage = jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date) VALUES (?, 'Regular', ?, ?) "
                + "RETURNING id", Long.class, cycle, TODAY.minusMonths(2), TODAY.plusMonths(2));
        r1 = mockExam(stage, "Regular 1", TODAY.minusDays(30));
        long r2 = mockExam(stage, "Regular 2", TODAY.minusDays(20));
        r3 = mockExam(stage, "Regular 3", TODAY.plusDays(5));
        r1Letras = exam(r1, letras);
        long r1Ciencias = exam(r1, ciencias);
        long r2Letras = exam(r2, letras);
        exam(r2, ciencias);
        r3Letras = exam(r3, letras);
        r3Ciencias = exam(r3, ciencias);

        long closedCycle = jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) "
                + "VALUES ('Pasado', '2025-01-01', '2025-06-30', 'CLOSED') RETURNING id", Long.class);
        long closedStage = jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date) "
                + "VALUES (?, 'Final', '2025-01-01', '2025-06-30') RETURNING id", Long.class, closedCycle);
        closedMockExam = mockExam(closedStage, "Antiguo", LocalDate.parse("2025-03-01"));

        ana = enroll("Ana", "Arias", stage, "Letras", silvia);
        anaStudent = jdbc.queryForObject("SELECT student_id FROM student_stage WHERE id = ?", Long.class, ana);
        beto = enroll("Beto", "Benítez", stage, null, silvia);
        carla = enroll("Carla", "Castro", stage, null, ruth);
        dario = enroll("Dario", "Duarte", stage, "Arquitectura", silvia);

        result(r1Letras, ana, true, 10, 5);
        result(r2Letras, ana, false, 0, 0); // did not attend: never the base of a suggestion
        result(r1Ciencias, beto, true, 8, 9);
    }

    @AfterEach
    void emptyTables() {
        jdbc.update("DELETE FROM goal");
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

    private long mockExam(long stageId, String name, LocalDate date) {
        return jdbc.queryForObject("INSERT INTO mock_exam (stage_id, name, exam_date) VALUES (?, ?, ?) RETURNING id",
                Long.class, stageId, name, date);
    }

    private long exam(long mockExamId, long typeId) {
        return jdbc.queryForObject("INSERT INTO exam (mock_exam_id, exam_type_id) VALUES (?, ?) RETURNING id",
                Long.class, mockExamId, typeId);
    }

    private long enroll(String firstName, String lastName, long stageId, String career, Long tutorId) {
        long student = jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat) VALUES (?, ?) RETURNING id",
                Long.class, firstName, lastName);
        long enrollment = jdbc.queryForObject("INSERT INTO student_stage (student_id, stage_id, career_id) "
                + "VALUES (?, ?, (SELECT id FROM career WHERE name = ?)) RETURNING id", Long.class, student, stageId, career);
        jdbc.update("INSERT INTO tutor_assignment (student_stage_id, tutor_id) VALUES (?, ?)", enrollment, tutorId);
        return enrollment;
    }

    private void result(long examId, long enrollmentId, boolean attended, int reading, int math) {
        long result = jdbc.queryForObject("INSERT INTO exam_result (exam_id, student_stage_id, total_score, attended) "
                + "VALUES (?, ?, 500, ?) RETURNING id", Long.class, examId, enrollmentId, attended);
        jdbc.update("INSERT INTO exam_course_result (exam_result_id, course_id, correct, wrong, blank) VALUES "
                + "(?, 1, ?, 0, 0), (?, 3, ?, 0, 0)", result, reading, result, math);
    }

    private ResultActions as(String role, long userId, MockHttpServletRequestBuilder request) throws Exception {
        AuthenticatedUser principal = () -> userId;
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return mockMvc.perform(request.with(authentication(auth)).with(csrf()).contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions sheet(String role, long userId, long mockExamId, Long tutorId) throws Exception {
        var request = get("/api/v1/goals/sheet").param("mockExamId", String.valueOf(mockExamId));
        if (tutorId != null) {
            request.param("tutorId", String.valueOf(tutorId));
        }
        return as(role, userId, request);
    }

    private ResultActions save(String role, long userId, long mockExamId, String... entries) throws Exception {
        return as(role, userId, put("/api/v1/goals/sheet")
                .content("{\"mockExamId\":" + mockExamId + ",\"goals\":[" + String.join(",", entries) + "]}"));
    }

    private static String entry(long enrollmentId, long areaId, Integer target) {
        return "{\"enrollmentId\":" + enrollmentId + ",\"areaId\":" + areaId + ",\"targetCorrect\":" + target + "}";
    }

    private Integer goals() {
        return jdbc.queryForObject("SELECT count(*) FROM goal", Integer.class);
    }

    @Test
    void aTutorGetsTheirGroupWithTheExamAndTheSuggestion() throws Exception {
        sheet("TUTOR", silvia, r3, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.mockExamName").value("Regular 3"))
                .andExpect(jsonPath("$.areas[0].areaName").value("Lectura"))
                .andExpect(jsonPath("$.rows.length()").value(3))
                // Ana: her career's type; the suggestion skips Regular 2, which she did not attend
                .andExpect(jsonPath("$.rows[0].fullName").value("Arias, Ana"))
                .andExpect(jsonPath("$.rows[0].examId").value(r3Letras))
                .andExpect(jsonPath("$.rows[0].examTypeName").value("Letras"))
                .andExpect(jsonPath("$.rows[0].attended").isEmpty())
                .andExpect(jsonPath("$.rows[0].goals[0].suggested").value(12))
                .andExpect(jsonPath("$.rows[0].goals[0].basedOnExamName").value("Regular 1"))
                .andExpect(jsonPath("$.rows[0].goals[1].suggested").value(7))
                .andExpect(jsonPath("$.rows[0].goals[0].target").isEmpty())
                // Beto: no career, so the type of the last exam he sat
                .andExpect(jsonPath("$.rows[1].examId").value(r3Ciencias))
                .andExpect(jsonPath("$.rows[1].goals[1].suggested").value(11))
                // Dario: Arquitectura, which Regular 3 does not have
                .andExpect(jsonPath("$.rows[2].examId").isEmpty())
                .andExpect(jsonPath("$.rows[2].examTypeName").value("Arquitectura"))
                .andExpect(jsonPath("$.rows[2].noExamReason").value("EXAM_TYPE_MISSING"))
                .andExpect(jsonPath("$.rows[2].goals[0].suggested").isEmpty());
    }

    @Test
    void anAdminSeesEveryoneOrOneTutorsGroup() throws Exception {
        sheet("ADMIN", admin, r3, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(4))
                // Carla never sat an exam and has no career
                .andExpect(jsonPath("$.rows[2].fullName").value("Castro, Carla"))
                .andExpect(jsonPath("$.rows[2].noExamReason").value("NO_EXAM_TYPE"))
                .andExpect(jsonPath("$.rows[2].tutorName").value("Salas, Ruth"));
        sheet("ADMIN", admin, r3, ruth).andExpect(status().isOk()).andExpect(jsonPath("$.rows.length()").value(1));
        sheet("TUTOR", silvia, r3, ruth).andExpect(status().isForbidden());
        sheet("COORDINADOR", admin, r3, null).andExpect(status().isForbidden());
        sheet("ADMIN", admin, closedMockExam, null).andExpect(status().isConflict());
        sheet("ADMIN", admin, 999_999, null).andExpect(status().isNotFound());
    }

    @Test
    void savesTheGroupsGoalsAllOrNothing() throws Exception {
        save("TUTOR", silvia, r3, entry(ana, LECTURA, 12), entry(ana, MATEMATICA, 8), entry(beto, LECTURA, 10))
                .andExpect(status().isNoContent());
        sheet("TUTOR", silvia, r3, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].goals[0].target").value(12))
                .andExpect(jsonPath("$.rows[0].goals[1].target").value(8))
                .andExpect(jsonPath("$.rows[1].goals[0].target").value(10))
                .andExpect(jsonPath("$.rows[1].goals[1].target").isEmpty());

        // editing one and emptying another
        save("TUTOR", silvia, r3, entry(ana, LECTURA, 14), entry(ana, MATEMATICA, null)).andExpect(status().isNoContent());
        assertThat(goals()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT target_correct FROM goal WHERE student_stage_id = ? AND area_id = 1",
                Integer.class, ana)).isEqualTo(14);

        // Dario has no exam in Regular 3: Ana's change is not saved either
        save("TUTOR", silvia, r3, entry(ana, LECTURA, 20), entry(dario, LECTURA, 5)).andExpect(status().isConflict());
        // Carla is Ruth's
        save("TUTOR", silvia, r3, entry(carla, LECTURA, 5)).andExpect(status().isForbidden());
        save("TUTOR", silvia, r3, entry(ana, LECTURA, -1)).andExpect(status().isBadRequest());
        save("TUTOR", silvia, closedMockExam, entry(ana, LECTURA, 5)).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT target_correct FROM goal WHERE student_stage_id = ? AND area_id = 1",
                Integer.class, ana)).isEqualTo(14);
        assertThat(goals()).isEqualTo(2);
    }

    @Test
    void aPastExamShowsWhetherTheGoalWasMet() throws Exception {
        // set afterwards, for analysis
        save("ADMIN", admin, r1, entry(ana, LECTURA, 9), entry(ana, MATEMATICA, 6)).andExpect(status().isNoContent());
        sheet("ADMIN", admin, r1, silvia).andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].examId").value(r1Letras))
                .andExpect(jsonPath("$.rows[0].attended").value(true))
                .andExpect(jsonPath("$.rows[0].goals[0].actual").value(10))
                // nothing sat before Regular 1: no suggestion
                .andExpect(jsonPath("$.rows[0].goals[0].suggested").isEmpty());

        as("TUTOR", silvia, get("/api/v1/goals").param("studentId", String.valueOf(anaStudent))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].mockExamName").value("Regular 1"))
                .andExpect(jsonPath("$[0].examTypeName").value("Letras"))
                .andExpect(jsonPath("$[0].areaName").value("Lectura"))
                .andExpect(jsonPath("$[0].actual").value(10))
                .andExpect(jsonPath("$[0].met").value(true))
                .andExpect(jsonPath("$[1].areaName").value("Matemáticas"))
                .andExpect(jsonPath("$[1].met").value(false));
        as("TUTOR", ruth, get("/api/v1/goals").param("studentId", String.valueOf(anaStudent))).andExpect(status().isForbidden());
        as("COORDINADOR", admin, get("/api/v1/goals").param("studentId", String.valueOf(anaStudent))).andExpect(status().isForbidden());
    }

    @Test
    void didNotAttendIsNeverEvaluated() throws Exception {
        long r2 = jdbc.queryForObject("SELECT id FROM mock_exam WHERE name = 'Regular 2'", Long.class);
        save("TUTOR", silvia, r2, entry(ana, LECTURA, 11)).andExpect(status().isNoContent());
        sheet("TUTOR", silvia, r2, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].attended").value(false))
                .andExpect(jsonPath("$.rows[0].goals[0].actual").isEmpty())
                .andExpect(jsonPath("$.rows[0].goals[0].suggested").value(12));
        as("ADMIN", admin, get("/api/v1/goals").param("studentId", String.valueOf(anaStudent))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attended").value(false))
                .andExpect(jsonPath("$[0].met").isEmpty());
    }

    @Test
    void listsTheMockExamsOfTheActiveCycle() throws Exception {
        as("TUTOR", silvia, get("/api/v1/goals/mock-exams")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(r1))
                .andExpect(jsonPath("$[2].name").value("Regular 3"))
                .andExpect(jsonPath("$[2].stageName").value("Regular"));
    }
}
