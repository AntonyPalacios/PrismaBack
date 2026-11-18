package pe.com.edu.prisma.students;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
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
 * Current tutor and stage in the students list, the tutor filter (ADMIN
 * only), "my students" for a TUTOR and who edits a student. The active
 * cycle has a past stage and the stage in force (by today's date); the
 * people are invented (tutors with emails @prueba.pe, removed afterwards).
 *
 * Today's stage: Lucía (Ríos), Mateo (Vargas), Valeria (enrolled, no tutor),
 * Diego (withdrawn, Ríos closed) and Camila (inactive student, Ríos).
 * Sofía was only in the past stage (Ríos) and Ana is not enrolled at all.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class StudentsByTutorApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long rios;
    private long vargas;
    private long lucia;
    private long valeria;
    private long cycle;

    @BeforeEach
    void seed() {
        emptyTables();
        LocalDate today = LocalDate.now(ZoneId.of("America/Lima"));
        cycle = jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) "
                + "VALUES ('Ciclo prueba', ?, ?, 'ACTIVE') RETURNING id", Long.class,
                today.minusDays(60), today.plusDays(60));
        long past = stage("Etapa 1", today.minusDays(60), today.minusDays(11), "CLOSED");
        long current = stage("Etapa 2", today.minusDays(10), today.plusDays(10), "ACTIVE");
        rios = appUser("maria.rios@prueba.pe", "María", "Ríos", "Paredes", "TUTOR");
        vargas = appUser("julia.vargas@prueba.pe", "Julia", "Vargas", null, "TUTOR");

        lucia = student("Lucía", "Campos", true);
        tutor(enroll(lucia, current, true), rios, false);
        tutor(enroll(student("Mateo", "Chávez", true), current, true), vargas, false);
        valeria = student("Valeria", "Díaz", true);
        enroll(valeria, current, true);
        tutor(enroll(student("Diego", "Mendoza", true), current, false), rios, true);
        tutor(enroll(student("Camila", "Rojas", false), current, true), rios, false);
        tutor(enroll(student("Sofía", "Paredes", true), past, true), rios, false);
        student("Ana", "Zapata", true);
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

    private long stage(String name, LocalDate start, LocalDate end, String status) {
        return jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date, status) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, cycle, name, start, end, status);
    }

    private long appUser(String email, String firstName, String lastNamePat, String lastNameMat, String role) {
        long id = jdbc.queryForObject("INSERT INTO app_user (email, first_name, last_name_pat, last_name_mat) "
                + "VALUES (?, ?, ?, ?) RETURNING id", Long.class, email, firstName, lastNamePat, lastNameMat);
        jdbc.update("INSERT INTO user_role (user_id, role_id) SELECT ?, id FROM role WHERE name = ?", id, role);
        return id;
    }

    private long student(String firstName, String lastNamePat, boolean active) {
        return jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat, active) VALUES (?, ?, ?) RETURNING id",
                Long.class, firstName, lastNamePat, active);
    }

    private long enroll(long student, long stage, boolean active) {
        return jdbc.queryForObject("INSERT INTO student_stage (student_id, stage_id, active) VALUES (?, ?, ?) RETURNING id",
                Long.class, student, stage, active);
    }

    private void tutor(long enrollment, long tutor, boolean closed) {
        jdbc.update("INSERT INTO tutor_assignment (student_stage_id, tutor_id, valid_from, valid_to) "
                + "VALUES (?, ?, now() - interval '5 days', " + (closed ? "now()" : "null") + ")", enrollment, tutor);
    }

    /** A request as a user with that id and those roles (the principal the JWT filter would set). */
    private ResultActions as(Long userId, MockHttpServletRequestBuilder request, String... roles) throws Exception {
        AuthenticatedUser principal = () -> userId;
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                Arrays.stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());
        return mockMvc.perform(request.with(authentication(auth)).with(csrf()).contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return as(1L, request, "ADMIN");
    }

    private List<String> names(ResultActions result) throws Exception {
        String json = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[*].firstName");
    }

    private static String body(String firstName, String lastNamePat) {
        return "{\"firstName\":\"" + firstName + "\",\"lastNamePat\":\"" + lastNamePat + "\"}";
    }

    @Test
    void everyStudentCarriesTheStageAndTutorOfTheirCurrentEnrollment() throws Exception {
        asAdmin(get("/api/v1/students").param("includeInactive", "true"))
                .andExpect(jsonPath("$[?(@.firstName == 'Lucía')].currentStageName").value("Etapa 2"))
                .andExpect(jsonPath("$[?(@.firstName == 'Lucía')].tutorId").value((int) rios))
                .andExpect(jsonPath("$[?(@.firstName == 'Lucía')].tutorName").value("Ríos Paredes, María"))
                // enrolled today without a tutor
                .andExpect(jsonPath("$[?(@.firstName == 'Valeria')].currentStageName").value("Etapa 2"))
                .andExpect(jsonPath("$[?(@.firstName == 'Valeria')].tutorId").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())))
                // withdrawn today, only in a past stage, or not enrolled: no current enrollment
                .andExpect(jsonPath("$[?(@.firstName == 'Diego')].currentStageName").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())))
                .andExpect(jsonPath("$[?(@.firstName == 'Sofía')].tutorName").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())))
                .andExpect(jsonPath("$[?(@.firstName == 'Ana')].currentStageName").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())));

        // the file carries it too
        asAdmin(get("/api/v1/students/" + lucia))
                .andExpect(jsonPath("$.currentStageName").value("Etapa 2"))
                .andExpect(jsonPath("$.tutorName").value("Ríos Paredes, María"));
    }

    @Test
    void everyStudentCarriesTheCareerOfTheirCurrentEnrollmentAndItsExamType() throws Exception {
        // Biomédica counts as Ciencias: the group is the career's exam type
        jdbc.update("UPDATE student_stage SET career_id = (SELECT id FROM career WHERE name = 'Biomédica') "
                + "WHERE student_id = ? AND active", lucia);
        long ciencias = jdbc.queryForObject("SELECT id FROM exam_type WHERE name = 'Ciencias'", Long.class);
        asAdmin(get("/api/v1/students"))
                .andExpect(jsonPath("$[?(@.firstName == 'Lucía')].careerName").value("Biomédica"))
                .andExpect(jsonPath("$[?(@.firstName == 'Lucía')].examTypeId").value((int) ciencias))
                .andExpect(jsonPath("$[?(@.firstName == 'Lucía')].examTypeName").value("Ciencias"))
                // enrolled without a career, or without a current enrollment
                .andExpect(jsonPath("$[?(@.firstName == 'Valeria')].careerName").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())))
                .andExpect(jsonPath("$[?(@.firstName == 'Ana')].examTypeId").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())));
    }

    @Test
    void withoutAnActiveCycleNobodyHasATutor() throws Exception {
        jdbc.update("UPDATE stage SET status = 'CLOSED'");
        jdbc.update("UPDATE cycle SET status = 'CLOSED'");

        assertThat(names(asAdmin(get("/api/v1/students").param("withoutTutor", "true"))))
                .containsExactly("Lucía", "Mateo", "Valeria", "Diego", "Sofía", "Ana");
    }

    @Test
    void adminFiltersByTutorOrWithoutTutor() throws Exception {
        assertThat(names(asAdmin(get("/api/v1/students").param("tutorId", String.valueOf(rios)))))
                .containsExactly("Lucía");
        assertThat(names(asAdmin(get("/api/v1/students").param("tutorId", String.valueOf(rios))
                .param("includeInactive", "true")))).containsExactly("Lucía", "Camila");
        assertThat(names(asAdmin(get("/api/v1/students").param("withoutTutor", "true"))))
                .containsExactly("Valeria", "Diego", "Sofía", "Ana");
        assertThat(names(asAdmin(get("/api/v1/students").param("withoutTutor", "true").param("search", "dia"))))
                .containsExactly("Valeria");
    }

    @Test
    void aTutorOnlyListsTheirCurrentStudents() throws Exception {
        assertThat(names(as(rios, get("/api/v1/students"), "TUTOR"))).containsExactly("Lucía");
        assertThat(names(as(rios, get("/api/v1/students").param("includeInactive", "true"), "TUTOR")))
                .containsExactly("Lucía", "Camila");
        assertThat(names(as(rios, get("/api/v1/students").param("search", "mateo"), "TUTOR"))).isEmpty();
        assertThat(names(as(vargas, get("/api/v1/students"), "TUTOR"))).containsExactly("Mateo");

        // but opens any student's file
        as(rios, get("/api/v1/students/" + valeria), "TUTOR").andExpect(status().isOk());
    }

    @Test
    void anAdminWhoIsAlsoATutorGetsTheAdminView() throws Exception {
        assertThat(names(as(rios, get("/api/v1/students"), "ADMIN", "TUTOR"))).hasSize(6);
    }

    @Test
    void onlyAnAdminFiltersByTutor() throws Exception {
        as(rios, get("/api/v1/students").param("tutorId", String.valueOf(vargas)), "TUTOR")
                .andExpect(status().isForbidden());
        as(rios, get("/api/v1/students").param("withoutTutor", "true"), "TUTOR").andExpect(status().isForbidden());
        as(9L, get("/api/v1/students").param("tutorId", String.valueOf(rios)), "COORDINADOR")
                .andExpect(status().isForbidden());
    }

    @Test
    void aCoordinatorListsEveryoneWithTheirTutor() throws Exception {
        as(9L, get("/api/v1/students"), "COORDINADOR")
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[?(@.firstName == 'Mateo')].tutorName").value("Vargas, Julia"));
    }

    @Test
    void onlyAnAdminOrTheCurrentTutorEditsAStudent() throws Exception {
        as(rios, put("/api/v1/students/" + lucia).content(body("Lucía", "Campos")), "TUTOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorName").value("Ríos Paredes, María"));
        as(vargas, put("/api/v1/students/" + lucia).content(body("Lucía", "Campos")), "TUTOR")
                .andExpect(status().isForbidden());
        // no tutor yet: only an admin, whoever registered the student
        as(rios, put("/api/v1/students/" + valeria).content(body("Valeria", "Díaz")), "TUTOR")
                .andExpect(status().isForbidden());
        asAdmin(put("/api/v1/students/" + valeria).content(body("Valeria", "Díaz"))).andExpect(status().isOk());
    }
}
