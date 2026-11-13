package pe.com.edu.prisma.tutoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Tutor of an enrollment (REQ-TUTORING-01). One active cycle with one
 * stage and one enrolled student, inserted straight into the database;
 * the tutors are invented users (emails @prueba.pe, removed afterwards).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class TutorAssignmentApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long cycle;
    private long enrollment;
    private long rios;
    private long vargas;

    @BeforeEach
    void seed() {
        emptyTables();
        cycle = jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) "
                + "VALUES ('2026-I', '2026-01-01', '2026-12-31', 'ACTIVE') RETURNING id", Long.class);
        long stage = jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date, status) "
                + "VALUES (?, 'Etapa 1', '2026-01-01', '2026-06-30', 'ACTIVE') RETURNING id", Long.class, cycle);
        long student = jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat) "
                + "VALUES ('Lucía', 'Quispe') RETURNING id", Long.class);
        enrollment = jdbc.queryForObject("INSERT INTO student_stage (student_id, stage_id) VALUES (?, ?) RETURNING id",
                Long.class, student, stage);
        rios = appUser("maria.rios@prueba.pe", "María", "Ríos", "Paredes", true, "TUTOR");
        vargas = appUser("julia.vargas@prueba.pe", "Julia", "Vargas", null, true, "TUTOR");
    }

    @AfterEach
    void emptyTables() {
        jdbc.update("DELETE FROM tutor_assignment");
        jdbc.update("DELETE FROM student_stage");
        jdbc.update("DELETE FROM student");
        jdbc.update("DELETE FROM stage");
        jdbc.update("DELETE FROM cycle");
        jdbc.update("DELETE FROM user_role WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@prueba.pe')");
        jdbc.update("DELETE FROM app_user WHERE email LIKE '%@prueba.pe'");
    }

    private long appUser(String email, String firstName, String lastNamePat, String lastNameMat, boolean active,
            String role) {
        long id = jdbc.queryForObject("INSERT INTO app_user (email, first_name, last_name_pat, last_name_mat, active) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, email, firstName, lastNamePat, lastNameMat, active);
        jdbc.update("INSERT INTO user_role (user_id, role_id) SELECT ?, id FROM role WHERE name = ?", id, role);
        return id;
    }

    private String assignments() {
        return "/api/v1/enrollments/" + enrollment + "/tutor-assignments";
    }

    private ResultActions as(String role, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user("someone").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions assign(long tutorId) throws Exception {
        return as("ADMIN", post(assignments()).content("{\"tutorUserId\":" + tutorId + "}"));
    }

    private String history() throws Exception {
        return as("ADMIN", get(assignments())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void adminAssignsTheFirstTutor(CapturedOutput output) throws Exception {
        String json = assign(rios)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.enrollmentId").value(enrollment))
                .andExpect(jsonPath("$.tutorId").value(rios))
                .andExpect(jsonPath("$.tutorName").value("Ríos Paredes, María"))
                .andExpect(jsonPath("$.validFrom").exists())
                .andExpect(jsonPath("$.validTo").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();

        assertThat(output.getOut()).contains("Tutor assigned: assignmentId=" + id + " enrollmentId=" + enrollment
                + " tutorId=" + rios + " closedAssignmentId=null");
    }

    @Test
    void reassigningClosesTheCurrentAssignmentAndKeepsTheHistory() throws Exception {
        assign(rios).andExpect(status().isCreated());
        assign(vargas).andExpect(status().isCreated());

        String json = history();
        assertThat(JsonPath.<List<String>>read(json, "$[*].tutorName")).containsExactly("Vargas, Julia", "Ríos Paredes, María");
        assertThat(JsonPath.<Object>read(json, "$[0].validTo")).isNull();
        // the previous one is closed exactly when the new one starts
        assertThat(JsonPath.<String>read(json, "$[1].validTo")).isEqualTo(JsonPath.read(json, "$[0].validFrom"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tutor_assignment WHERE valid_to IS NULL", Long.class))
                .isEqualTo(1);
    }

    @Test
    void choosingTheCurrentTutorAgainChangesNothing() throws Exception {
        String first = assign(rios).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        assign(rios)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((Integer) JsonPath.read(first, "$.id")));
        assertThat(JsonPath.<List<Object>>read(history(), "$")).hasSize(1);
    }

    @Test
    void onlyAnActiveTutorCanBeAssigned() throws Exception {
        long inactive = appUser("ana.soto@prueba.pe", "Ana", "Soto", null, false, "TUTOR");
        long coordinator = appUser("jorge.arce@prueba.pe", "Jorge", "Arce", null, true, "COORDINADOR");

        assign(inactive).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El usuario elegido no es un tutor activo."));
        assign(coordinator).andExpect(status().isConflict());
        assertThat(JsonPath.<List<Object>>read(history(), "$")).isEmpty();
    }

    @Test
    void aWithdrawnOrClosedCycleEnrollmentGetsNoTutor() throws Exception {
        jdbc.update("UPDATE cycle SET status = 'CLOSED'");
        assign(rios).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta matrícula es de un ciclo cerrado: ya no se puede modificar."));

        jdbc.update("UPDATE cycle SET status = 'ACTIVE'");
        jdbc.update("UPDATE student_stage SET active = false");
        assign(rios).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta matrícula está retirada: ya no se puede modificar."));
    }

    @Test
    void withdrawingFromTheStageEndsTheCurrentTutor() throws Exception {
        assign(rios).andExpect(status().isCreated());

        as("ADMIN", delete("/api/v1/enrollments/" + enrollment)).andExpect(status().isNoContent());

        String json = history();
        assertThat(JsonPath.<Object>read(json, "$[0].validTo")).isNotNull();
        as("ADMIN", get("/api/v1/enrollments").param("studentId", studentId()))
                .andExpect(jsonPath("$[0].tutorId").doesNotExist());
    }

    @Test
    void theEnrollmentListCarriesTheCurrentTutor() throws Exception {
        as("TUTOR", get("/api/v1/enrollments").param("studentId", studentId()))
                .andExpect(jsonPath("$[0].tutorId").doesNotExist())
                .andExpect(jsonPath("$[0].tutorName").doesNotExist());

        assign(rios);
        assign(vargas);

        as("TUTOR", get("/api/v1/enrollments").param("studentId", studentId()))
                .andExpect(jsonPath("$[0].tutorId").value(vargas))
                .andExpect(jsonPath("$[0].tutorName").value("Vargas, Julia"));
    }

    @Test
    void aTutorReadsTheHistoryButOnlyAdminAssigns() throws Exception {
        assign(rios).andExpect(status().isCreated());

        as("TUTOR", get(assignments())).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        as("TUTOR", post(assignments()).content("{\"tutorUserId\":" + vargas + "}")).andExpect(status().isForbidden());
        as("COORDINADOR", get(assignments())).andExpect(status().isForbidden());
        as("COORDINADOR", post(assignments()).content("{\"tutorUserId\":" + vargas + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingOrUnknownDataIsRejected() throws Exception {
        as("ADMIN", post(assignments()).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Elige el tutor."));
        assign(999999).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el usuario solicitado."));
        as("ADMIN", post("/api/v1/enrollments/999999/tutor-assignments").content("{\"tutorUserId\":" + rios + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos la matrícula solicitada."));
        as("ADMIN", get("/api/v1/enrollments/999999/tutor-assignments")).andExpect(status().isNotFound());
    }

    private String studentId() {
        return String.valueOf(jdbc.queryForObject("SELECT student_id FROM student_stage WHERE id = ?", Long.class, enrollment));
    }
}
