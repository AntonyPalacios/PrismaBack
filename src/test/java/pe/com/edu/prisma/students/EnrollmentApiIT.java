package pe.com.edu.prisma.students;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.com.edu.prisma.config.ContainersConfig;

/**
 * Enrollments (REQ-STUDENTS-02). Cycles and stages are inserted straight
 * into the database so each test gets the states it needs:
 * a closed 2025-I (one closed stage), an active 2026-I (a closed and an
 * active stage) and a planned 2026-II (one planned stage). People are invented.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class EnrollmentApiIT {

    private static final String ENROLLMENTS = "/api/v1/enrollments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long closedCycleStage;
    private long pastStage;
    private long currentStage;
    private long plannedStage;
    private long letras;
    private long ciencias;
    private long student;

    @BeforeEach
    void seed() {
        emptyTables();
        long closedCycle = cycle("2025-I", "2025-01-01", "2025-06-30", "CLOSED");
        closedCycleStage = stage(closedCycle, "Etapa 1", "2025-01-01", "2025-03-31", "CLOSED");
        long activeCycle = cycle("2026-I", "2026-01-01", "2026-06-30", "ACTIVE");
        pastStage = stage(activeCycle, "Etapa 1", "2026-01-01", "2026-02-28", "CLOSED");
        currentStage = stage(activeCycle, "Etapa 2", "2026-03-01", "2026-06-30", "ACTIVE");
        long plannedCycle = cycle("2026-II", "2026-07-01", "2026-12-31", "PLANNED");
        plannedStage = stage(plannedCycle, "Etapa 1", "2026-07-01", "2026-09-30", "PLANNED");
        letras = jdbc.queryForObject("SELECT id FROM career WHERE name = 'Letras'", Long.class);
        ciencias = jdbc.queryForObject("SELECT id FROM career WHERE name = 'Ciencias'", Long.class);
        student = student("Lucía", "Quispe", true);
    }

    @AfterEach
    void emptyTables() {
        jdbc.update("DELETE FROM exam");
        jdbc.update("DELETE FROM student_stage");
        jdbc.update("DELETE FROM student");
        jdbc.update("DELETE FROM stage");
        jdbc.update("DELETE FROM cycle");
        jdbc.update("DELETE FROM career WHERE name LIKE 'Prueba %'");
    }

    private long cycle(String name, String start, String end, String status) {
        return jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) "
                + "VALUES (?, ?::date, ?::date, ?) RETURNING id", Long.class, name, start, end, status);
    }

    private long stage(long cycle, String name, String start, String end, String status) {
        return jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date, status) "
                + "VALUES (?, ?, ?::date, ?::date, ?) RETURNING id", Long.class, cycle, name, start, end, status);
    }

    private long student(String firstName, String lastNamePat, boolean active) {
        return jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat, active) VALUES (?, ?, ?) RETURNING id",
                Long.class, firstName, lastNamePat, active);
    }

    private long inactiveCareer() {
        return jdbc.queryForObject("INSERT INTO career (name, exam_type_id, active) VALUES ('Prueba inactiva', 1, false) "
                + "RETURNING id", Long.class);
    }

    private static String body(Long studentId, Long stageId, Long careerId) {
        return "{\"studentId\":" + studentId + ",\"stageId\":" + stageId + ",\"careerId\":" + careerId + "}";
    }

    private static String career(Long careerId) {
        return "{\"careerId\":" + careerId + "}";
    }

    private ResultActions as(String role, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user("someone").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return as("ADMIN", request);
    }

    private ResultActions enroll(Long studentId, Long stageId, Long careerId) throws Exception {
        return asAdmin(post(ENROLLMENTS).content(body(studentId, stageId, careerId)));
    }

    private long enrolled(long stageId, Long careerId) throws Exception {
        String json = enroll(student, stageId, careerId).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private ResultActions history(String role) throws Exception {
        return as(role, get(ENROLLMENTS).param("studentId", String.valueOf(student)));
    }

    @Test
    void adminEnrollsAStudentWithACareer(CapturedOutput output) throws Exception {
        String json = enroll(student, currentStage, letras)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(ENROLLMENTS + "/")))
                .andExpect(jsonPath("$.studentId").value(student))
                .andExpect(jsonPath("$.cycleName").value("2026-I"))
                .andExpect(jsonPath("$.cycleStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.stageId").value(currentStage))
                .andExpect(jsonPath("$.stageName").value("Etapa 2"))
                .andExpect(jsonPath("$.stageStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.careerId").value(letras))
                .andExpect(jsonPath("$.careerName").value("Letras"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();

        assertThat(output.getOut()).contains("Student enrolled: enrollmentId=" + id
                + " studentId=" + student + " stageId=" + currentStage);
    }

    @Test
    void theCareerIsOptionalAndCanBeSetChangedAndClearedLater() throws Exception {
        long id = enrolled(currentStage, null);

        asAdmin(put(ENROLLMENTS + "/" + id).content(career(letras)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerName").value("Letras"));
        asAdmin(put(ENROLLMENTS + "/" + id).content(career(ciencias)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerName").value("Ciencias"));
        asAdmin(put(ENROLLMENTS + "/" + id).content(career(null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerId").doesNotExist());
    }

    @Test
    void aStudentCanBeEnrolledAheadInAPlannedCycle() throws Exception {
        enroll(student, plannedStage, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cycleStatus").value("PLANNED"));
    }

    @Test
    void theHistoryListsEveryEnrollmentNewestStageFirst() throws Exception {
        long past = enrolled(pastStage, ciencias);
        long planned = enrolled(plannedStage, null);
        long current = enrolled(currentStage, letras);
        asAdmin(delete(ENROLLMENTS + "/" + current)).andExpect(status().isNoContent());
        // someone else's enrollment never shows up
        enroll(student("Mateo", "Rojas", true), currentStage, null).andExpect(status().isCreated());

        String json = history("TUTOR").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<Integer> ids = JsonPath.read(json, "$[*].id");
        assertThat(ids).containsExactly((int) planned, (int) current, (int) past);
        assertThat(JsonPath.<List<Boolean>>read(json, "$[*].active")).containsExactly(true, false, true);
    }

    @Test
    void aStudentHasAtMostOneEnrollmentPerStageEvenAfterWithdrawing() throws Exception {
        long id = enrolled(currentStage, null);

        enroll(student, currentStage, letras)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El alumno ya tiene una matrícula en esa etapa."));
        asAdmin(delete(ENROLLMENTS + "/" + id)).andExpect(status().isNoContent());
        enroll(student, currentStage, null).andExpect(status().isConflict());
    }

    @Test
    void nobodyEnrollsInAStageOfAClosedCycle() throws Exception {
        enroll(student, closedCycleStage, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No se puede matricular en una etapa de un ciclo cerrado."));
    }

    @Test
    void anInactiveStudentCannotBeEnrolled() throws Exception {
        long retired = student("Valeria", "Díaz", false);

        enroll(retired, currentStage, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este alumno está dado de baja: reactívalo antes de matricularlo."));
    }

    @Test
    void anInactiveCareerCannotBeChosenButMayStayWhereItWas() throws Exception {
        long inactive = inactiveCareer();

        enroll(student, currentStage, inactive)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La carrera elegida está inactiva: elige otra."));

        long id = enrolled(currentStage, null);
        asAdmin(put(ENROLLMENTS + "/" + id).content(career(inactive))).andExpect(status().isConflict());

        // chosen while active, deactivated later: saving it again is fine
        jdbc.update("UPDATE student_stage SET career_id = ? WHERE id = ?", inactive, id);
        asAdmin(put(ENROLLMENTS + "/" + id).content(career(inactive))).andExpect(status().isOk());
    }

    @Test
    void aWithdrawnEnrollmentIsReadOnly(CapturedOutput output) throws Exception {
        long id = enrolled(currentStage, letras);

        asAdmin(delete(ENROLLMENTS + "/" + id)).andExpect(status().isNoContent());
        assertThat(output.getOut()).contains("Student withdrawn from stage: enrollmentId=" + id);

        String readOnly = "Esta matrícula está retirada: ya no se puede modificar.";
        asAdmin(put(ENROLLMENTS + "/" + id).content(career(ciencias)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(readOnly));
        asAdmin(delete(ENROLLMENTS + "/" + id)).andExpect(status().isConflict());
        // withdrawing keeps the row and its career
        history("ADMIN").andExpect(jsonPath("$[0].careerName").value("Letras"));
    }

    @Test
    void anEnrollmentOfAClosedCycleIsReadOnly() throws Exception {
        long id = enrolled(currentStage, letras);
        jdbc.update("UPDATE stage SET status = 'CLOSED'");
        jdbc.update("UPDATE cycle SET status = 'CLOSED'");

        String readOnly = "Esta matrícula es de un ciclo cerrado: ya no se puede modificar.";
        asAdmin(put(ENROLLMENTS + "/" + id).content(career(ciencias)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(readOnly));
        asAdmin(delete(ENROLLMENTS + "/" + id)).andExpect(status().isConflict());
        history("ADMIN")
                .andExpect(jsonPath("$[0].cycleStatus").value("CLOSED"))
                .andExpect(jsonPath("$[0].stageStatus").value("CLOSED"));
    }

    @Test
    void aTutorEnrollsEditsAndWithdraws() throws Exception {
        String json = as("TUTOR", post(ENROLLMENTS).content(body(student, currentStage, null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();

        as("TUTOR", put(ENROLLMENTS + "/" + id).content(career(letras))).andExpect(status().isOk());
        as("TUTOR", delete(ENROLLMENTS + "/" + id)).andExpect(status().isNoContent());
    }

    @Test
    void aCoordinatorDoesNotSeeEnrollments() throws Exception {
        long id = enrolled(currentStage, null);

        history("COORDINADOR").andExpect(status().isForbidden());
        as("COORDINADOR", post(ENROLLMENTS).content(body(student, plannedStage, null)))
                .andExpect(status().isForbidden());
        as("COORDINADOR", put(ENROLLMENTS + "/" + id).content(career(letras))).andExpect(status().isForbidden());
        as("COORDINADOR", delete(ENROLLMENTS + "/" + id)).andExpect(status().isForbidden());
    }

    @Test
    void missingOrUnknownDataIsRejectedInSpanish() throws Exception {
        asAdmin(get(ENROLLMENTS)).andExpect(status().isBadRequest());
        enroll(null, currentStage, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indica el alumno que se matricula."));
        enroll(student, null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Elige la etapa de la matrícula."));

        enroll(999999L, currentStage, null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el alumno solicitado."));
        enroll(student, 999999L, null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos la etapa solicitada."));
        enroll(student, currentStage, 999999L).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos la carrera solicitada."));
        asAdmin(get(ENROLLMENTS).param("studentId", "999999")).andExpect(status().isNotFound());
        asAdmin(put(ENROLLMENTS + "/999999").content(career(null))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos la matrícula solicitada."));
        asAdmin(delete(ENROLLMENTS + "/999999")).andExpect(status().isNotFound());
    }

    @Test
    void readingRequiresASession() throws Exception {
        mockMvc.perform(get(ENROLLMENTS).param("studentId", "1")).andExpect(status().isUnauthorized());
    }
}
