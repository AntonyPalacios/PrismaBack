package pe.com.edu.prisma.dashboards;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pe.com.edu.prisma.config.ContainersConfig;
import pe.com.edu.prisma.security.AuthenticatedUser;

/**
 * A student's score evolution (REQ-DASHBOARDS-01, agreements of 2026-11-16).
 * Everything is inserted straight into the database; every person is
 * invented. Lucía sat a Letras exam in the closed cycle 2025-II and two in
 * the active 2026-I; Silvia (seed) is her tutor today.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class DashboardApiIT {

    private static final long LETRAS = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long admin;
    private long silvia;
    private long lucia;
    private long mateo;
    private long oldExam;
    private long regular1;
    private long regular2;

    @BeforeEach
    void seed() {
        emptyTables();
        admin = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'apalaciosc0109@gmail.com'", Long.class);
        silvia = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'silvia.valleg@prisma.edu.pe'", Long.class);
        long closedStage = stage(cycle("2025-II", "2025-07-01", "2025-12-31", "CLOSED"), "Repaso", "2025-07-01", "2025-12-31");
        long stage = stage(cycle("2026-I", "2026-01-01", "2026-12-31", "ACTIVE"), "Regular", "2026-03-01", "2026-06-30");
        oldExam = exam(closedStage, "Repaso 5", "2025-11-08");
        regular1 = exam(stage, "Regular 1", "2026-03-14");
        regular2 = exam(stage, "Regular 2", "2026-03-21");

        lucia = student("Lucía", "Quispe");
        mateo = student("Mateo", "Rojas");
        long diego = student("Diego", "Paredes");
        long ana = student("Ana", "Torres");
        long luciaOld = enroll(lucia, closedStage);
        long luciaNow = enroll(lucia, stage);
        long mateoNow = enroll(mateo, stage);
        long diegoNow = enroll(diego, stage);
        long anaNow = enroll(ana, stage);
        jdbc.update("INSERT INTO tutor_assignment (student_stage_id, tutor_id) VALUES (?, ?)", luciaNow, silvia);

        long old = result(oldExam, luciaOld, "500.00", true, "480.5", "520.5");
        course(old, 1, 15, 10, 3);
        course(old, 2, 4, 3, 1);
        // Regular 1: Diego 700, Lucía and Mateo tied at 650, Ana did not attend
        long diegoFirst = result(regular1, diegoNow, "700.00", true, "700", "700");
        course(diegoFirst, 1, 10, 10, 8);
        course(diegoFirst, 2, 0, 8, 0); // no correct answer at all: a zero that counts
        course(diegoFirst, 3, 4, 2, 12);
        course(diegoFirst, 6, 1, 0, 6);
        long first = result(regular1, luciaNow, "650.00", true, "640.5", "660");
        course(first, 1, 20, 6, 2);
        course(first, 2, 5, 2, 1);
        course(first, 3, 10, 6, 2);
        course(first, 6, 3, 3, 1); // Estadística: only evaluated in Regular 1
        long mateoFirst = result(regular1, mateoNow, "650.00", true, "650", "650");
        course(mateoFirst, 1, 15, 5, 8);
        course(mateoFirst, 2, 2, 2, 4);
        course(mateoFirst, 3, 6, 6, 6);
        course(mateoFirst, 6, 2, 2, 3);
        long anaFirst = result(regular1, anaNow, "80.00", false, "0", "0");
        course(anaFirst, 1, 0, 0, 28);
        // Regular 2: Lucía 722.76 first, Diego 600; Mateo did not attend
        long second = result(regular2, luciaNow, "722.76", true, "787.5", "819.4");
        course(second, 1, 26, 2, 0);
        course(second, 2, 7, 1, 0);
        course(second, 3, 17, 1, 0);
        long diegoSecond = result(regular2, diegoNow, "600.00", true, "600", "600");
        course(diegoSecond, 1, 12, 8, 8);
        course(diegoSecond, 2, 3, 5, 0);
        course(diegoSecond, 3, 5, 5, 8);
        result(regular2, mateoNow, "80.00", false, "0", "0");
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

    /** A mock exam with its Letras exam; returns the exam's id. */
    private long exam(long stageId, String name, String date) {
        long mockExam = jdbc.queryForObject("INSERT INTO mock_exam (stage_id, name, exam_date) VALUES (?, ?, ?::date) "
                + "RETURNING id", Long.class, stageId, name, date);
        return jdbc.queryForObject("INSERT INTO exam (mock_exam_id, exam_type_id) VALUES (?, ?) RETURNING id",
                Long.class, mockExam, LETRAS);
    }

    private long student(String firstName, String lastNamePat) {
        return jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat) VALUES (?, ?) RETURNING id",
                Long.class, firstName, lastNamePat);
    }

    private long enroll(long studentId, long stageId) {
        return jdbc.queryForObject("INSERT INTO student_stage (student_id, stage_id) VALUES (?, ?) RETURNING id",
                Long.class, studentId, stageId);
    }

    private long result(long examId, long enrollmentId, String total, boolean attended, String reading, String math) {
        long result = jdbc.queryForObject("INSERT INTO exam_result (exam_id, student_stage_id, total_score, attended) "
                + "VALUES (?, ?, ?::numeric, ?) RETURNING id", Long.class, examId, enrollmentId, total, attended);
        jdbc.update("INSERT INTO exam_area_result (exam_result_id, area_id, score) VALUES (?, 1, ?::numeric), (?, 2, ?::numeric)",
                result, reading, result, math);
        return result;
    }

    /** Correct, wrong and blank answers of one course (1 Lectura; 2 to 6 are Matemática's). */
    private void course(long resultId, long courseId, int correct, int wrong, int blank) {
        jdbc.update("INSERT INTO exam_course_result (exam_result_id, course_id, correct, wrong, blank) "
                + "VALUES (?, ?, ?, ?, ?)", resultId, courseId, correct, wrong, blank);
    }

    private ResultActions dashboard(String name, String role, long userId, long studentId, boolean all) throws Exception {
        AuthenticatedUser principal = () -> userId;
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return mockMvc.perform(get("/api/v1/dashboards/students/" + studentId + "/" + name)
                .param("includeAllCycles", String.valueOf(all)).with(authentication(auth)).with(csrf()));
    }

    private ResultActions evolution(String role, long userId, long studentId, boolean all) throws Exception {
        AuthenticatedUser principal = () -> userId;
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return mockMvc.perform(get("/api/v1/dashboards/students/" + studentId + "/score-evolution")
                .param("includeAllCycles", String.valueOf(all)).with(authentication(auth)).with(csrf()));
    }

    @Test
    void theActiveCycleByDateWithTheExamRangeTheMeritAndTheAreaScores() throws Exception {
        evolution("ADMIN", admin, lucia, false).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].examId").value(regular1))
                .andExpect(jsonPath("$[0].examName").value("Regular 1"))
                .andExpect(jsonPath("$[0].examTypeName").value("Letras"))
                .andExpect(jsonPath("$[0].examDate").value("2026-03-14"))
                .andExpect(jsonPath("$[0].cycleName").value("2026-I"))
                .andExpect(jsonPath("$[0].studentScore").value(650.0))
                // tied with Mateo behind Diego: both second, of the three who attended
                .andExpect(jsonPath("$[0].merit").value(2))
                .andExpect(jsonPath("$[0].meritOf").value(3))
                .andExpect(jsonPath("$[0].examMax").value(700.0))
                // Ana's 80 does not count: she did not attend
                .andExpect(jsonPath("$[0].examMin").value(650.0))
                .andExpect(jsonPath("$[0].areaScores[0].areaName").value("Lectura"))
                .andExpect(jsonPath("$[0].areaScores[0].score").value(640.5))
                .andExpect(jsonPath("$[0].areaScores[1].score").value(660.0))
                .andExpect(jsonPath("$[1].examId").value(regular2))
                .andExpect(jsonPath("$[1].studentScore").value(722.76))
                .andExpect(jsonPath("$[1].merit").value(1))
                .andExpect(jsonPath("$[1].meritOf").value(2))
                .andExpect(jsonPath("$[1].examMax").value(722.76))
                .andExpect(jsonPath("$[1].examMin").value(600.0));
    }

    @Test
    void earlierCyclesOnlyWhenAsked() throws Exception {
        evolution("ADMIN", admin, lucia, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].examId").value(oldExam))
                .andExpect(jsonPath("$[0].cycleName").value("2025-II"))
                // alone in that exam: she is its max and its min
                .andExpect(jsonPath("$[0].merit").value(1))
                .andExpect(jsonPath("$[0].meritOf").value(1))
                .andExpect(jsonPath("$[0].examMax").value(500.0))
                .andExpect(jsonPath("$[0].examMin").value(500.0));
    }

    @Test
    void anExamTheStudentDidNotAttendIsLeftOut() throws Exception {
        evolution("ADMIN", admin, mateo, false).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].examId").value(regular1))
                .andExpect(jsonPath("$[0].merit").value(2));
        long nobody = student("Rosa", "Huamán");
        evolution("ADMIN", admin, nobody, true).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aTutorOnlySeesTheirCurrentStudentsAndACoordinatorNone() throws Exception {
        evolution("TUTOR", silvia, lucia, false).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        // Mateo has no tutor; and another tutor is not Lucía's
        evolution("TUTOR", silvia, mateo, false).andExpect(status().isForbidden());
        evolution("TUTOR", admin, lucia, false).andExpect(status().isForbidden());
        evolution("COORDINADOR", admin, lucia, false).andExpect(status().isForbidden());
        evolution("ADMIN", admin, 999_999, false).andExpect(status().isNotFound());
    }

    @Test
    void eachCourseWithTheExamsThatEvaluatedIt() throws Exception {
        dashboard("course-effectiveness", "ADMIN", admin, lucia, false).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].courseId").value(1))
                .andExpect(jsonPath("$[0].courseName").value("Lectura"))
                .andExpect(jsonPath("$[0].points.length()").value(2))
                .andExpect(jsonPath("$[0].points[0].examId").value(regular1))
                .andExpect(jsonPath("$[0].points[0].examName").value("Regular 1"))
                .andExpect(jsonPath("$[0].points[0].examDate").value("2026-03-14"))
                .andExpect(jsonPath("$[0].points[0].cycleName").value("2026-I"))
                .andExpect(jsonPath("$[0].points[0].correct").value(20))
                .andExpect(jsonPath("$[0].points[0].wrong").value(6))
                .andExpect(jsonPath("$[0].points[0].blank").value(2))
                .andExpect(jsonPath("$[0].points[1].correct").value(26))
                // Estadística was not evaluated in Regular 2: one point, never a zero
                .andExpect(jsonPath("$[3].courseId").value(6))
                .andExpect(jsonPath("$[3].points.length()").value(1))
                .andExpect(jsonPath("$[3].points[0].examId").value(regular1));
        dashboard("course-effectiveness", "ADMIN", admin, lucia, true)
                .andExpect(jsonPath("$[0].points.length()").value(3))
                .andExpect(jsonPath("$[0].points[0].examId").value(oldExam))
                .andExpect(jsonPath("$[0].points[0].cycleName").value("2025-II"));
    }

    @Test
    void correctAndWrongAnswersPerAreaAndInTotal() throws Exception {
        dashboard("area-distribution", "ADMIN", admin, lucia, false).andExpect(status().isOk())
                .andExpect(jsonPath("$.areas.length()").value(2))
                .andExpect(jsonPath("$.areas[0].areaName").value("Lectura"))
                .andExpect(jsonPath("$.areas[0].points[0].examId").value(regular1))
                .andExpect(jsonPath("$.areas[0].points[0].correct").value(20))
                .andExpect(jsonPath("$.areas[0].points[0].wrong").value(6))
                // Matemática: the sum of its courses in each exam
                .andExpect(jsonPath("$.areas[1].points[0].correct").value(18))
                .andExpect(jsonPath("$.areas[1].points[0].wrong").value(11))
                .andExpect(jsonPath("$.areas[1].points[1].correct").value(24))
                .andExpect(jsonPath("$.areas[1].points[1].wrong").value(2))
                .andExpect(jsonPath("$.totals.length()").value(2))
                .andExpect(jsonPath("$.totals[0].correct").value(38))
                .andExpect(jsonPath("$.totals[0].wrong").value(17))
                .andExpect(jsonPath("$.totals[1].examName").value("Regular 2"))
                .andExpect(jsonPath("$.totals[1].correct").value(50))
                .andExpect(jsonPath("$.totals[1].wrong").value(4));
        dashboard("area-distribution", "ADMIN", admin, lucia, true).andExpect(jsonPath("$.totals.length()").value(3));
    }

    @Test
    void theNewChartsFollowTheSamePermissionsAndEmptyStates() throws Exception {
        for (String name : List.of("course-effectiveness", "area-distribution")) {
            dashboard(name, "TUTOR", silvia, lucia, false).andExpect(status().isOk());
            dashboard(name, "TUTOR", silvia, mateo, false).andExpect(status().isForbidden());
            dashboard(name, "COORDINADOR", admin, lucia, false).andExpect(status().isForbidden());
            dashboard(name, "ADMIN", admin, 999_999, false).andExpect(status().isNotFound());
        }
        long nobody = student("Rosa", "Huamán");
        dashboard("course-effectiveness", "ADMIN", admin, nobody, true).andExpect(jsonPath("$.length()").value(0));
        dashboard("area-distribution", "ADMIN", admin, nobody, true)
                .andExpect(jsonPath("$.areas.length()").value(0))
                .andExpect(jsonPath("$.totals.length()").value(0));
    }

    private ResultActions group(String role, long userId, String query) throws Exception {
        AuthenticatedUser principal = () -> userId;
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return mockMvc.perform(get("/api/v1/dashboards/group-comparison" + query).with(authentication(auth)).with(csrf()));
    }

    @Test
    void theAcademyByExamWithAverageLowestAndHighestPerAreaAndTheCourseAverages() throws Exception {
        group("ADMIN", admin, "?examTypeId=" + LETRAS).andExpect(status().isOk())
                // only the active cycle, by date
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].examId").value(regular1))
                .andExpect(jsonPath("$[0].examName").value("Regular 1"))
                .andExpect(jsonPath("$[0].examDate").value("2026-03-14"))
                // Diego, Lucía and Mateo; Ana did not attend
                .andExpect(jsonPath("$[0].students").value(3))
                .andExpect(jsonPath("$[0].areas[0].areaName").value("Lectura"))
                .andExpect(jsonPath("$[0].areas[0].correctAvg").value(15.0))
                .andExpect(jsonPath("$[0].areas[0].correctMin").value(10))
                .andExpect(jsonPath("$[0].areas[0].correctMax").value(20))
                .andExpect(jsonPath("$[0].areas[0].wrongAvg").value(7.0))
                .andExpect(jsonPath("$[0].areas[0].wrongMin").value(5))
                .andExpect(jsonPath("$[0].areas[0].wrongMax").value(10))
                // Matemática: each student's sum of its courses (5, 18 and 10 correct)
                .andExpect(jsonPath("$[0].areas[1].correctAvg").value(11.0))
                .andExpect(jsonPath("$[0].areas[1].correctMin").value(5))
                .andExpect(jsonPath("$[0].areas[1].correctMax").value(18))
                .andExpect(jsonPath("$[0].areas[1].wrongAvg").value(10.33))
                .andExpect(jsonPath("$[0].areas[1].wrongMin").value(10))
                .andExpect(jsonPath("$[0].areas[1].wrongMax").value(11))
                // per course; Diego's zero counts for the average
                .andExpect(jsonPath("$[0].courses.length()").value(4))
                .andExpect(jsonPath("$[0].courses[1].courseId").value(2))
                .andExpect(jsonPath("$[0].courses[1].correctAvg").value(2.33))
                .andExpect(jsonPath("$[0].courses[3].courseName").value("Estadística"))
                .andExpect(jsonPath("$[0].courses[3].correctAvg").value(2.0))
                // Regular 2 did not evaluate Estadística, and Mateo did not attend
                .andExpect(jsonPath("$[1].examId").value(regular2))
                .andExpect(jsonPath("$[1].students").value(2))
                .andExpect(jsonPath("$[1].courses.length()").value(3))
                .andExpect(jsonPath("$[1].areas[0].correctAvg").value(19.0))
                .andExpect(jsonPath("$[1].areas[0].correctMin").value(12))
                .andExpect(jsonPath("$[1].areas[0].correctMax").value(26));
        // an exam type without exams in the active cycle
        group("ADMIN", admin, "?examTypeId=1").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aTutorAlwaysGetsTheirOwnGroupAndAnAdminMayPickOne() throws Exception {
        // Silvia's group is Lucía alone
        for (ResultActions own : List.of(group("TUTOR", silvia, "?examTypeId=" + LETRAS),
                group("ADMIN", admin, "?examTypeId=" + LETRAS + "&tutorId=" + silvia))) {
            own.andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].students").value(1))
                    .andExpect(jsonPath("$[0].areas[0].correctAvg").value(20.0))
                    .andExpect(jsonPath("$[0].areas[0].correctMin").value(20))
                    .andExpect(jsonPath("$[0].areas[0].correctMax").value(20));
        }
        // a tutor without students: nothing, never the academy
        group("TUTOR", admin, "?examTypeId=" + LETRAS).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        group("ADMIN", admin, "?examTypeId=" + LETRAS + "&tutorId=" + admin).andExpect(jsonPath("$.length()").value(0));

        group("TUTOR", silvia, "?examTypeId=" + LETRAS + "&tutorId=" + admin).andExpect(status().isForbidden());
        group("COORDINADOR", admin, "?examTypeId=" + LETRAS).andExpect(status().isForbidden());
        group("ADMIN", admin, "?examTypeId=999999").andExpect(status().isNotFound());
        group("ADMIN", admin, "?examTypeId=" + LETRAS + "&tutorId=999999").andExpect(status().isNotFound());
        group("ADMIN", admin, "").andExpect(status().isBadRequest());
    }
}
