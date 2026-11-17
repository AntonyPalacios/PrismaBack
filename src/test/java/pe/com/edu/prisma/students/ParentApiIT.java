package pe.com.edu.prisma.students;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * Guardians (REQ-STUDENTS-03, agreements of 2026-11-17). The active cycle
 * spans today: Ana and her brother Beto are Silvia's (seed) students, Carla
 * is Ruth's (invented). Rosa is Ana's and Beto's mother. Every person is invented.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class ParentApiIT {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Lima"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long admin;
    private long silvia;
    private long ruth;
    private long ana;
    private long beto;
    private long carla;
    private long rosa;

    @BeforeEach
    void seed() {
        emptyTables();
        admin = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'apalaciosc0109@gmail.com'", Long.class);
        silvia = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'silvia.valleg@prisma.edu.pe'", Long.class);
        ruth = jdbc.queryForObject("INSERT INTO app_user (email, first_name, last_name_pat) "
                + "VALUES ('ruth.apoderados@prueba.pe', 'Ruth', 'Salas') RETURNING id", Long.class);
        jdbc.update("INSERT INTO user_role (user_id, role_id) SELECT ?, id FROM role WHERE name = 'TUTOR'", ruth);
        long cycle = jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) VALUES ('Actual', ?, ?, 'ACTIVE') "
                + "RETURNING id", Long.class, TODAY.minusMonths(2), TODAY.plusMonths(2));
        long stage = jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date) VALUES (?, 'Regular', ?, ?) "
                + "RETURNING id", Long.class, cycle, TODAY.minusMonths(2), TODAY.plusMonths(2));
        ana = enroll("Ana", stage, silvia);
        beto = enroll("Beto", stage, silvia);
        carla = enroll("Carla", stage, ruth);
        rosa = jdbc.queryForObject("INSERT INTO parent (full_name, phone, email) VALUES ('Rosa Ramos Díaz', '999 112 334', NULL) "
                + "RETURNING id", Long.class);
        jdbc.update("INSERT INTO student_parent (student_id, parent_id, relationship_type) VALUES (?, ?, 'MADRE'), (?, ?, 'MADRE')",
                ana, rosa, beto, rosa);
    }

    @AfterEach
    void emptyTables() {
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

    private long enroll(String firstName, long stageId, long tutorId) {
        long student = jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat) VALUES (?, 'Ramos') RETURNING id",
                Long.class, firstName);
        long enrollment = jdbc.queryForObject("INSERT INTO student_stage (student_id, stage_id) VALUES (?, ?) RETURNING id",
                Long.class, student, stageId);
        jdbc.update("INSERT INTO tutor_assignment (student_stage_id, tutor_id) VALUES (?, ?)", enrollment, tutorId);
        return student;
    }

    private ResultActions as(String role, long userId, MockHttpServletRequestBuilder request) throws Exception {
        AuthenticatedUser principal = () -> userId;
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return mockMvc.perform(request.with(authentication(auth)).with(csrf()).contentType(MediaType.APPLICATION_JSON));
    }

    private static String newParent(String fullName, String phone, String email, String relationship) {
        return "{\"parent\":{\"fullName\":\"" + fullName + "\",\"phone\":\"" + phone + "\",\"email\":"
                + (email == null ? "null" : "\"" + email + "\"") + "},\"relationshipType\":\"" + relationship + "\"}";
    }

    private static String existing(long parentId, String relationship) {
        return "{\"parentId\":" + parentId + ",\"relationshipType\":\"" + relationship + "\"}";
    }

    private Integer links(long studentId) {
        return jdbc.queryForObject("SELECT count(*) FROM student_parent WHERE student_id = ?", Integer.class, studentId);
    }

    @Test
    void listsTheGuardiansWithTheOtherStudentsTheyLookAfter() throws Exception {
        as("TUTOR", ruth, get("/api/v1/students/" + ana + "/parents")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].parentId").value(rosa))
                .andExpect(jsonPath("$[0].fullName").value("Rosa Ramos Díaz"))
                .andExpect(jsonPath("$[0].phone").value("999 112 334"))
                .andExpect(jsonPath("$[0].relationshipType").value("MADRE"))
                .andExpect(jsonPath("$[0].otherStudents[0]").value("Ramos, Beto"));
        as("ADMIN", admin, get("/api/v1/students/" + carla + "/parents")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        as("COORDINADOR", admin, get("/api/v1/students/" + ana + "/parents")).andExpect(status().isForbidden());
        as("ADMIN", admin, get("/api/v1/students/999999/parents")).andExpect(status().isNotFound());
    }

    @Test
    void theCurrentTutorRegistersAndLinksANewGuardianAtOnce(CapturedOutput output) throws Exception {
        as("TUTOR", silvia, post("/api/v1/students/" + ana + "/parents")
                .content(newParent("  Jorge   Ramos Pinto ", "+51 987 654 321", "", "PADRE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Jorge Ramos Pinto"))
                .andExpect(jsonPath("$.phone").value("+51 987 654 321"))
                .andExpect(jsonPath("$.email").isEmpty())
                .andExpect(jsonPath("$.relationshipType").value("PADRE"))
                .andExpect(jsonPath("$.otherStudents.length()").value(0));
        assertThat(links(ana)).isEqualTo(2);
        // contact data never reaches the logs in clear
        assertThat(output).doesNotContain("987 654 321").doesNotContain("Jorge Ramos");
    }

    @Test
    void anExistingGuardianLinksToASiblingOnce() throws Exception {
        as("TUTOR", ruth, post("/api/v1/students/" + carla + "/parents").content(existing(rosa, "OTRO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationshipType").value("OTRO"))
                .andExpect(jsonPath("$.otherStudents.length()").value(2));
        as("TUTOR", ruth, post("/api/v1/students/" + carla + "/parents").content(existing(rosa, "OTRO")))
                .andExpect(status().isConflict());
        as("ADMIN", admin, post("/api/v1/students/" + carla + "/parents").content(existing(999_999, "OTRO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsIncompleteGuardians() throws Exception {
        String path = "/api/v1/students/" + ana + "/parents";
        as("ADMIN", admin, post(path).content(newParent("Jorge Ramos", "", null, "PADRE"))).andExpect(status().isBadRequest());
        as("ADMIN", admin, post(path).content(newParent("", "987654321", null, "PADRE"))).andExpect(status().isBadRequest());
        as("ADMIN", admin, post(path).content(newParent("Jorge Ramos", "12345", null, "PADRE"))).andExpect(status().isBadRequest());
        as("ADMIN", admin, post(path).content(newParent("Jorge Ramos", "987654321", "no-es-correo", "PADRE")))
                .andExpect(status().isBadRequest());
        as("ADMIN", admin, post(path).content(newParent("Jorge Ramos", "987654321", null, "TIO"))).andExpect(status().isBadRequest());
        as("ADMIN", admin, post(path).content("{\"relationshipType\":\"PADRE\"}")).andExpect(status().isBadRequest());
        as("ADMIN", admin, post(path).content("{\"parentId\":" + rosa + ",\"parent\":{\"fullName\":\"X Y\",\"phone\":\"987654321\"},"
                + "\"relationshipType\":\"PADRE\"}")).andExpect(status().isBadRequest());
        assertThat(links(ana)).isEqualTo(1);
    }

    @Test
    void onlyTheAdminOrTheCurrentTutorChangeThem() throws Exception {
        // Ruth does not tutor Ana: she reads, but changes nothing
        as("TUTOR", ruth, post("/api/v1/students/" + ana + "/parents").content(newParent("Jorge Ramos", "987654321", null, "PADRE")))
                .andExpect(status().isForbidden());
        as("TUTOR", ruth, delete("/api/v1/students/" + ana + "/parents/" + rosa)).andExpect(status().isForbidden());
        as("TUTOR", ruth, put("/api/v1/parents/" + rosa).content("{\"fullName\":\"Rosa Ramos\",\"phone\":\"999112334\"}"))
                .andExpect(status().isForbidden());
        as("COORDINADOR", admin, delete("/api/v1/students/" + ana + "/parents/" + rosa)).andExpect(status().isForbidden());
        assertThat(links(ana)).isEqualTo(1);

        // a deactivated student keeps their guardians as they are
        jdbc.update("UPDATE student SET active = false WHERE id = ?", ana);
        as("ADMIN", admin, delete("/api/v1/students/" + ana + "/parents/" + rosa)).andExpect(status().isConflict());
    }

    @Test
    void unlinkingKeepsTheGuardianAndTheirOtherStudents() throws Exception {
        as("TUTOR", silvia, delete("/api/v1/students/" + ana + "/parents/" + rosa)).andExpect(status().isNoContent());
        assertThat(links(ana)).isZero();
        assertThat(links(beto)).isEqualTo(1);
        as("TUTOR", silvia, delete("/api/v1/students/" + ana + "/parents/" + rosa)).andExpect(status().isNotFound());
        // still there to be linked again
        as("TUTOR", ruth, get("/api/v1/parents").param("search", "rosa")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(rosa));
    }

    @Test
    void editingAGuardianChangesItForEveryStudent() throws Exception {
        as("TUTOR", silvia, put("/api/v1/parents/" + rosa)
                .content("{\"fullName\":\"Rosa Ramos Díaz\",\"phone\":\"988 000 111\",\"email\":\"rosa@prueba.pe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("988 000 111"));
        as("ADMIN", admin, get("/api/v1/students/" + beto + "/parents")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("rosa@prueba.pe"));
        as("ADMIN", admin, put("/api/v1/parents/999999").content("{\"fullName\":\"X Y\",\"phone\":\"987654321\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchesByNameOrPhoneDigits() throws Exception {
        as("TUTOR", ruth, get("/api/v1/parents").param("search", "RAMOS")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        as("TUTOR", ruth, get("/api/v1/parents").param("search", "999112")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Rosa Ramos Díaz"));
        as("TUTOR", ruth, get("/api/v1/parents").param("search", "r")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        as("TUTOR", ruth, get("/api/v1/parents").param("search", "%")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        as("COORDINADOR", admin, get("/api/v1/parents").param("search", "rosa")).andExpect(status().isForbidden());
    }
}
