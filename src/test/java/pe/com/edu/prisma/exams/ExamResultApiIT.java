package pe.com.edu.prisma.exams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import pe.com.edu.prisma.config.ContainersConfig;
import pe.com.edu.prisma.security.AuthenticatedUser;

/**
 * A student's result typed by hand (REQ-EXAMS-04, agreements of 2026-11-17).
 * Everything is inserted straight into the database; every person is
 * invented. Regular 1 (Arquitectura: courses 1 to 4) already has Diego's
 * imported result; Lucía (Silvia's student) and Mateo (no tutor) have none.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class ExamResultApiIT {

    private static final long ARQUITECTURA = 3;

    /** Arquitectura's four courses and both areas: Lucía's typed result. */
    private static final String BODY = """
            {"courseResults": [
              {"courseId": 1, "correct": 20, "wrong": 5, "blank": 3},
              {"courseId": 2, "correct": 6, "wrong": 1, "blank": 1},
              {"courseId": 3, "correct": 9, "wrong": 4, "blank": 5},
              {"courseId": 4, "correct": 2, "wrong": 0, "blank": 4}],
             "areaScores": [{"areaId": 1, "score": 640.5}, {"areaId": 2, "score": 600}],
             "totalScore": 586.4}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long admin;
    private long silvia;
    private long stage;
    private long regular1;
    private long luciaNow;
    private long mateoNow;
    private long diegoNow;

    @BeforeEach
    void seed() {
        emptyTables();
        admin = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'apalaciosc0109@gmail.com'", Long.class);
        silvia = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'silvia.valleg@prisma.edu.pe'", Long.class);
        stage = stage(cycle("2026-I", "2026-01-01", "2026-12-31", "ACTIVE"), "Regular", "2026-03-01", "2026-06-30");
        regular1 = exam(stage, "Regular 1", "2026-03-14");
        luciaNow = enroll(student("Lucía", "Quispe"), stage);
        mateoNow = enroll(student("Mateo", "Rojas"), stage);
        diegoNow = enroll(student("Diego", "Paredes"), stage);
        jdbc.update("INSERT INTO tutor_assignment (student_stage_id, tutor_id) VALUES (?, ?)", luciaNow, silvia);
        long diego = jdbc.queryForObject("INSERT INTO exam_result (exam_id, student_stage_id, total_score, attended, "
                + "total_correct, total_wrong, total_blank) VALUES (?, ?, 700, true, 30, 10, 2) RETURNING id",
                Long.class, regular1, diegoNow);
        jdbc.update("INSERT INTO exam_course_result (exam_result_id, course_id, correct, wrong, blank) VALUES "
                + "(?, 1, 20, 5, 1), (?, 2, 5, 2, 0), (?, 3, 4, 2, 1), (?, 4, 1, 1, 0)", diego, diego, diego, diego);
        jdbc.update("INSERT INTO exam_area_result (exam_result_id, area_id, score) VALUES (?, 1, 700), (?, 2, 700)",
                diego, diego);
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
    }

    private long cycle(String name, String start, String end, String status) {
        return jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) "
                + "VALUES (?, ?::date, ?::date, ?) RETURNING id", Long.class, name, start, end, status);
    }

    private long stage(long cycleId, String name, String start, String end) {
        return jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date) "
                + "VALUES (?, ?, ?::date, ?::date) RETURNING id", Long.class, cycleId, name, start, end);
    }

    /** A mock exam with its Arquitectura exam; returns the exam's id. */
    private long exam(long stageId, String name, String date) {
        long mockExam = jdbc.queryForObject("INSERT INTO mock_exam (stage_id, name, exam_date) VALUES (?, ?, ?::date) "
                + "RETURNING id", Long.class, stageId, name, date);
        return jdbc.queryForObject("INSERT INTO exam (mock_exam_id, exam_type_id) VALUES (?, ?) RETURNING id",
                Long.class, mockExam, ARQUITECTURA);
    }

    private long student(String firstName, String lastNamePat) {
        return jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat, dni) VALUES (?, ?, ?) RETURNING id",
                Long.class, firstName, lastNamePat, String.valueOf(70_000_000 + firstName.length() * 1000 + lastNamePat.length()));
    }

    private long enroll(long studentId, long stageId) {
        return jdbc.queryForObject("INSERT INTO student_stage (student_id, stage_id) VALUES (?, ?) RETURNING id",
                Long.class, studentId, stageId);
    }

    private UsernamePasswordAuthenticationToken as(String role, long userId) {
        AuthenticatedUser principal = () -> userId;
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private ResultActions save(String role, long userId, long examId, long enrollmentId, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/exams/" + examId + "/results/" + enrollmentId)
                .contentType(MediaType.APPLICATION_JSON).content(body).with(authentication(as(role, userId))).with(csrf()));
    }

    private ResultActions find(String role, long userId, long examId, long enrollmentId) throws Exception {
        return mockMvc.perform(get("/api/v1/exams/" + examId + "/results/" + enrollmentId)
                .with(authentication(as(role, userId))));
    }

    private ResultActions enrollments(String role, long userId, long examId) throws Exception {
        return mockMvc.perform(get("/api/v1/exams/" + examId + "/enrollments").with(authentication(as(role, userId))));
    }

    @Test
    void createsTheMissingResultAsTypedAndReadsItBack() throws Exception {
        find("ADMIN", admin, regular1, luciaNow).andExpect(status().isNotFound());
        save("ADMIN", admin, regular1, luciaNow, BODY).andExpect(status().isOk())
                .andExpect(jsonPath("$.examId").value(regular1))
                .andExpect(jsonPath("$.enrollmentId").value(luciaNow))
                .andExpect(jsonPath("$.courseResults.length()").value(4))
                .andExpect(jsonPath("$.courseResults[0].courseName").value("Lectura"))
                .andExpect(jsonPath("$.courseResults[0].correct").value(20))
                .andExpect(jsonPath("$.areaScores[0].areaName").value("Lectura"))
                .andExpect(jsonPath("$.areaScores[0].score").value(640.5))
                .andExpect(jsonPath("$.totalScore").value(586.4))
                .andExpect(jsonPath("$.attended").value(true))
                .andExpect(jsonPath("$.updatedByName").value("Palacios Coronado, Antony"))
                .andExpect(jsonPath("$.updatedAt").exists());
        find("ADMIN", admin, regular1, luciaNow).andExpect(status().isOk())
                .andExpect(jsonPath("$.courseResults[3].blank").value(4))
                .andExpect(jsonPath("$.areaScores[1].score").value(600.0));
        // the totals of answers are the sums of the courses
        Integer correct = jdbc.queryForObject("SELECT total_correct FROM exam_result WHERE student_stage_id = ?",
                Integer.class, luciaNow);
        assertThat(correct).isEqualTo(37);
    }

    @Test
    void overwritesAnExistingResultAndRecordsWhoDidIt() throws Exception {
        save("COORDINADOR", silvia, regular1, diegoNow, BODY).andExpect(status().isOk())
                .andExpect(jsonPath("$.courseResults[1].correct").value(6))
                .andExpect(jsonPath("$.totalScore").value(586.4))
                .andExpect(jsonPath("$.updatedByName").value("Valle Gómez, Silvia"));
        Integer rows = jdbc.queryForObject("SELECT count(*) FROM exam_course_result c JOIN exam_result r "
                + "ON r.id = c.exam_result_id WHERE r.student_stage_id = ?", Integer.class, diegoNow);
        assertThat(rows).isEqualTo(4);
    }

    @Test
    void noCorrectAndNoWrongAnswerMeansDidNotAttend() throws Exception {
        String absent = BODY.replaceAll("\"correct\": \\d+, \"wrong\": \\d+", "\"correct\": 0, \"wrong\": 0");
        save("ADMIN", admin, regular1, luciaNow, absent).andExpect(status().isOk())
                .andExpect(jsonPath("$.attended").value(false));
        // attended is never taken from the request
        save("ADMIN", admin, regular1, luciaNow, BODY.replace("\"totalScore\"", "\"attended\": false, \"totalScore\""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.attended").value(true));
    }

    @Test
    void aTutorOnlyTheStudentsTheyTutorAndACoordinatorAny() throws Exception {
        save("TUTOR", silvia, regular1, luciaNow, BODY).andExpect(status().isOk());
        find("TUTOR", silvia, regular1, luciaNow).andExpect(status().isOk());
        // Mateo has no tutor; the admin's id is not Lucía's tutor
        save("TUTOR", silvia, regular1, mateoNow, BODY).andExpect(status().isForbidden());
        find("TUTOR", admin, regular1, luciaNow).andExpect(status().isForbidden());
        save("COORDINADOR", admin, regular1, mateoNow, BODY).andExpect(status().isOk());
    }

    @Test
    void onlyOnAnExamWithResultsOfAnOpenCycleAndAnActiveEnrollmentOfItsStage() throws Exception {
        long regular2 = exam(stage, "Regular 2", "2026-03-21");
        save("ADMIN", admin, regular2, luciaNow, BODY).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este examen todavía no tiene resultados: primero importa el Excel."));

        long otherStage = stage(cycle("2025-II", "2025-07-01", "2025-12-31", "CLOSED"), "Repaso", "2025-07-01", "2025-12-31");
        long otherEnrollment = enroll(student("Ana", "Torres"), otherStage);
        save("ADMIN", admin, regular1, otherEnrollment, BODY).andExpect(status().isConflict());

        jdbc.update("UPDATE student_stage SET active = false WHERE id = ?", mateoNow);
        save("ADMIN", admin, regular1, mateoNow, BODY).andExpect(status().isConflict());

        jdbc.update("UPDATE cycle SET status = 'CLOSED' WHERE id = (SELECT cycle_id FROM stage WHERE id = ?)", stage);
        save("ADMIN", admin, regular1, luciaNow, BODY).andExpect(status().isConflict());
        // reading stays open
        find("ADMIN", admin, regular1, diegoNow).andExpect(status().isOk());
    }

    @Test
    void everyCourseOfTheTypeAndEveryAreaOnceWithNoNegatives() throws Exception {
        String missingCourse = BODY.replace(",\n  {\"courseId\": 4, \"correct\": 2, \"wrong\": 0, \"blank\": 4}]", "]");
        assertThat(missingCourse).doesNotContain("\"courseId\": 4");
        save("ADMIN", admin, regular1, luciaNow, missingCourse).andExpect(status().isBadRequest());
        // Trigonometría (5) is not evaluated in Arquitectura
        save("ADMIN", admin, regular1, luciaNow, BODY.replace("\"courseId\": 4", "\"courseId\": 5"))
                .andExpect(status().isBadRequest());
        save("ADMIN", admin, regular1, luciaNow, BODY.replace("\"areaId\": 2", "\"areaId\": 1"))
                .andExpect(status().isBadRequest());
        save("ADMIN", admin, regular1, luciaNow, BODY.replace("\"wrong\": 5", "\"wrong\": -1"))
                .andExpect(status().isBadRequest());
        save("ADMIN", admin, regular1, luciaNow, BODY.replace("\"totalScore\": 586.4", "\"totalScore\": null"))
                .andExpect(status().isBadRequest());
        find("ADMIN", admin, regular1, luciaNow).andExpect(status().isNotFound());
    }

    @Test
    void unknownExamOrEnrollment() throws Exception {
        save("ADMIN", admin, 999_999, luciaNow, BODY).andExpect(status().isNotFound());
        save("ADMIN", admin, regular1, 999_999, BODY).andExpect(status().isNotFound());
        enrollments("ADMIN", admin, 999_999).andExpect(status().isNotFound());
    }

    @Test
    void theStudentsOfTheStageToPickFromAndATutorOnlyTheirs() throws Exception {
        long withdrawn = enroll(student("Rosa", "Huamán"), stage);
        jdbc.update("UPDATE student_stage SET active = false WHERE id = ?", withdrawn);
        enrollments("COORDINADOR", admin, regular1).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fullName").value("Paredes, Diego"))
                .andExpect(jsonPath("$[0].hasResult").value(true))
                .andExpect(jsonPath("$[1].fullName").value("Quispe, Lucía"))
                .andExpect(jsonPath("$[1].enrollmentId").value(luciaNow))
                .andExpect(jsonPath("$[1].hasResult").value(false))
                .andExpect(jsonPath("$[2].fullName").value("Rojas, Mateo"));
        enrollments("TUTOR", silvia, regular1).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].enrollmentId").value(luciaNow));
    }
}
