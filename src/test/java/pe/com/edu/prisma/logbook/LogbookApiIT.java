package pe.com.edu.prisma.logbook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import pe.com.edu.prisma.config.ContainersConfig;
import pe.com.edu.prisma.security.AuthenticatedUser;

/**
 * A student's logbook (REQ-LOGBOOK-01/02, agreements of 2026-11-17). The
 * active cycle and its stage span today, so Silvia (seed) is Lucía's current
 * tutor; Ruth (invented) tutored her in an earlier, closed cycle.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class LogbookApiIT {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Lima"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long admin;
    private long silvia;
    private long ruth;
    private long lucia;
    private long mateo;

    @BeforeEach
    void seed() {
        emptyTables();
        admin = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'apalaciosc0109@gmail.com'", Long.class);
        silvia = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'silvia.valleg@prisma.edu.pe'", Long.class);
        ruth = jdbc.queryForObject("INSERT INTO app_user (email, first_name, last_name_pat, last_name_mat) "
                + "VALUES ('ruth.bitacora@prueba.pe', 'Ruth', 'Salas', 'Ríos') RETURNING id", Long.class);
        jdbc.update("INSERT INTO user_role (user_id, role_id) SELECT ?, id FROM role WHERE name = 'TUTOR'", ruth);

        long oldStage = stage(cycle("2025-I", TODAY.minusYears(1).minusMonths(2), TODAY.minusYears(1), "CLOSED"),
                TODAY.minusYears(1).minusMonths(2), TODAY.minusYears(1));
        long stage = stage(cycle("Actual", TODAY.minusMonths(1), TODAY.plusMonths(1), "ACTIVE"),
                TODAY.minusMonths(1), TODAY.plusMonths(1));
        lucia = jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat) VALUES ('Lucía', 'Quispe') RETURNING id", Long.class);
        mateo = jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat) VALUES ('Mateo', 'Rojas') RETURNING id", Long.class);
        jdbc.update("INSERT INTO tutor_assignment (student_stage_id, tutor_id) VALUES (?, ?)", enroll(lucia, oldStage), ruth);
        jdbc.update("INSERT INTO tutor_assignment (student_stage_id, tutor_id) VALUES (?, ?)", enroll(lucia, stage), silvia);
        enroll(mateo, stage); // no tutor
        // what Ruth wrote back then
        jdbc.update("INSERT INTO log_entry (student_id, author_id, motivo, session_date, meeting_with) "
                + "VALUES (?, ?, 'Ansiedad', ?, 'BOTH')",
                lucia, ruth, TODAY.minusYears(1).minusMonths(1));
    }

    @AfterEach
    void emptyTables() {
        jdbc.update("DELETE FROM log_entry");
        jdbc.update("DELETE FROM tutor_assignment");
        jdbc.update("DELETE FROM student_stage");
        jdbc.update("DELETE FROM student");
        jdbc.update("DELETE FROM stage");
        jdbc.update("DELETE FROM cycle");
        jdbc.update("DELETE FROM user_role WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@prueba.pe')");
        jdbc.update("DELETE FROM app_user WHERE email LIKE '%@prueba.pe'");
    }

    private long cycle(String name, LocalDate start, LocalDate end, String status) {
        return jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, name, start, end, status);
    }

    private long stage(long cycleId, LocalDate start, LocalDate end) {
        return jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date) VALUES (?, 'Regular', ?, ?) "
                + "RETURNING id", Long.class, cycleId, start, end);
    }

    private long enroll(long studentId, long stageId) {
        return jdbc.queryForObject("INSERT INTO student_stage (student_id, stage_id) VALUES (?, ?) RETURNING id",
                Long.class, studentId, stageId);
    }

    private UsernamePasswordAuthenticationToken as(String role, long userId) {
        AuthenticatedUser principal = () -> userId;
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private ResultActions write(String role, long userId, long studentId, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/students/" + studentId + "/logbook-entries")
                .contentType(MediaType.APPLICATION_JSON).content(body).with(authentication(as(role, userId))).with(csrf()));
    }

    private ResultActions read(String role, long userId, long studentId, String query) throws Exception {
        return mockMvc.perform(get("/api/v1/students/" + studentId + "/logbook-entries" + query)
                .with(authentication(as(role, userId))));
    }

    private static String entry(LocalDate date, String reason, String agreements, String observations, boolean confirm) {
        return """
                {"sessionDate": "%s", "meetingWith": "PARENT", "reason": %s, "agreements": %s, "observations": %s, "confirm": %s}
                """.formatted(date, quote(reason), quote(agreements), quote(observations), confirm);
    }

    private static String quote(String text) {
        return text == null ? "null" : "\"" + text + "\"";
    }

    @Test
    void theCurrentTutorWritesAndReadsTheWholeHistoryNewestFirst() throws Exception {
        write("TUTOR", silvia, lucia, entry(TODAY.minusDays(3), "Bajó en Álgebra", "Repasar 30 min diarios", "Muy motivada", false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Valle Gómez, Silvia"))
                .andExpect(jsonPath("$.sessionDate").value(TODAY.minusDays(3).toString()))
                .andExpect(jsonPath("$.meetingWith").value("PARENT"))
                .andExpect(jsonPath("$.reason").value("Bajó en Álgebra"))
                .andExpect(jsonPath("$.createdAt").exists());
        write("TUTOR", silvia, lucia, entry(TODAY, "Seguimiento", "Mantener", "Bien", false)).andExpect(status().isCreated());

        read("TUTOR", silvia, lucia, "").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.entries[0].reason").value("Seguimiento"))
                .andExpect(jsonPath("$.entries[1].reason").value("Bajó en Álgebra"))
                // a former tutor's entry is part of the student's history
                .andExpect(jsonPath("$.entries[2].authorName").value("Salas Ríos, Ruth"))
                .andExpect(jsonPath("$.entries[2].meetingWith").value("BOTH"));
        read("ADMIN", admin, lucia, "?page=1&size=2").andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].reason").value("Ansiedad"))
                .andExpect(jsonPath("$.hasMore").value(false));
        read("ADMIN", admin, lucia, "?size=2").andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    void anEmptyTextAsksForConfirmationAndAllEmptyIsRejected() throws Exception {
        write("TUTOR", silvia, lucia, entry(TODAY, "Charla corta", null, "  ", false)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.requiresConfirmation").value(true))
                .andExpect(jsonPath("$.emptyFields.length()").value(1))
                .andExpect(jsonPath("$.emptyFields[0]").value("agreements"))
                .andExpect(jsonPath("$.detail").value(containsString("compromisos / recomendaciones")))
                .andExpect(jsonPath("$.detail").value(containsString("no se podrá editar ni borrar")));
        write("TUTOR", silvia, lucia, entry(TODAY, "Charla corta", null, "  ", true)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.observations").doesNotExist());
        write("TUTOR", silvia, lucia, entry(TODAY, null, "", " ", true)).andExpect(status().isBadRequest());
        // observations are optional for good: no confirmation needed without them
        write("TUTOR", silvia, lucia, entry(TODAY, "Motivo", "Compromiso", null, false)).andExpect(status().isCreated());
        Integer count = jdbc.queryForObject("SELECT count(*) FROM log_entry WHERE student_id = ?", Integer.class, lucia);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void theSessionDateIsRequiredAndNeverInTheFuture() throws Exception {
        write("TUTOR", silvia, lucia, entry(TODAY.plusDays(1), "a", "b", "c", true)).andExpect(status().isBadRequest());
        write("TUTOR", silvia, lucia, "{\"meetingWith\": \"STUDENT\", \"reason\": \"a\", \"agreements\": \"b\"}")
                .andExpect(status().isBadRequest());
        // who the tutor met is required too, and only one of the three options
        write("TUTOR", silvia, lucia, entry(TODAY, "a", "b", "c", true).replace("\"PARENT\"", "null")).andExpect(status().isBadRequest());
        write("TUTOR", silvia, lucia, entry(TODAY, "a", "b", "c", true).replace("PARENT", "TEACHER")).andExpect(status().isBadRequest());
    }

    @Test
    void onlyTheCurrentTutorWritesAndOnlyTheyAndAnAdminRead() throws Exception {
        String body = entry(TODAY, "a", "b", "c", false);
        // not even an admin writes on the tutor's behalf
        write("ADMIN", admin, lucia, body).andExpect(status().isForbidden());
        write("TUTOR", admin, lucia, body).andExpect(status().isForbidden());
        // a former tutor loses access
        write("TUTOR", ruth, lucia, body).andExpect(status().isForbidden());
        read("TUTOR", ruth, lucia, "").andExpect(status().isForbidden());
        // Mateo has no tutor; a coordinator never sees the logbook
        write("TUTOR", silvia, mateo, body).andExpect(status().isForbidden());
        read("TUTOR", silvia, mateo, "").andExpect(status().isForbidden());
        read("COORDINADOR", admin, lucia, "").andExpect(status().isForbidden());
        read("ADMIN", admin, mateo, "").andExpect(status().isOk()).andExpect(jsonPath("$.entries.length()").value(0));
        read("ADMIN", admin, 999_999, "").andExpect(status().isNotFound());
    }
}
