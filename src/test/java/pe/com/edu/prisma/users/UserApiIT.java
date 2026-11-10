package pe.com.edu.prisma.users;

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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.com.edu.prisma.config.ContainersConfig;

/**
 * User management (REQ-SECURITY-02). Seed (V3): Antony (permanent ADMIN)
 * and Silvia (TUTOR). Tests create their own users with random emails and
 * never deactivate the seeded ones (other ITs log in with them).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class UserApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private static String uniqueEmail() {
        return "user." + UUID.randomUUID().toString().substring(0, 8) + "@prisma.edu.pe";
    }

    private static String createBody(String email, String firstName, String lastNamePat, String lastNameMat,
            String roles) {
        return "{\"email\":" + quote(email) + ",\"firstName\":" + quote(firstName)
                + ",\"lastNamePat\":" + quote(lastNamePat) + ",\"lastNameMat\":" + quote(lastNameMat)
                + ",\"roles\":" + roles + "}";
    }

    private static String updateBody(String firstName, String lastNamePat, String lastNameMat, String roles) {
        return "{\"firstName\":" + quote(firstName) + ",\"lastNamePat\":" + quote(lastNamePat)
                + ",\"lastNameMat\":" + quote(lastNameMat) + ",\"roles\":" + roles + "}";
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON));
    }

    private long createdId(String email, String roles) throws Exception {
        String json = asAdmin(post("/api/v1/users").content(createBody(email, "Ana", "Pérez", null, roles)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long permanentAdminId() {
        return userRepository.findByEmailIgnoreCase(User.PERMANENT_ADMIN_EMAIL).orElseThrow().getId();
    }

    @Test
    void adminCreatesAnActiveUserWithSeveralRolesAndANormalizedEmail() throws Exception {
        String email = uniqueEmail();
        asAdmin(post("/api/v1/users").content(
                createBody("  " + email.toUpperCase() + " ", " Julia ", " Vargas ", " ", "[\"TUTOR\",\"COORDINADOR\"]")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/users/")))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Julia"))
                .andExpect(jsonPath("$.lastNamePat").value("Vargas"))
                .andExpect(jsonPath("$.lastNameMat").doesNotExist())
                .andExpect(jsonPath("$.roles[0]").value("COORDINADOR"))
                .andExpect(jsonPath("$.roles[1]").value("TUTOR"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.photoUrl").doesNotExist())
                .andExpect(jsonPath("$.permanentAdmin").value(false));
    }

    @Test
    void duplicateEmailIgnoringCaseAnswers409() throws Exception {
        String email = uniqueEmail();
        createdId(email, "[\"TUTOR\"]");
        asAdmin(post("/api/v1/users").content(createBody(email.toUpperCase(), "Otro", "Nombre", null, "[\"TUTOR\"]")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un usuario con ese correo."));
    }

    @Test
    void invalidRequestsAnswer400InSpanish() throws Exception {
        asAdmin(post("/api/v1/users").content(createBody("", "Ana", "Pérez", null, "[\"TUTOR\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("correo")));
        asAdmin(post("/api/v1/users").content(createBody("no-es-correo", "Ana", "Pérez", null, "[\"TUTOR\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("formato válido")));
        asAdmin(post("/api/v1/users").content(createBody(uniqueEmail(), "", "Pérez", null, "[\"TUTOR\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("nombres")));
        asAdmin(post("/api/v1/users").content(createBody(uniqueEmail(), "Ana", "", null, "[\"TUTOR\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("apellido paterno")));
        asAdmin(post("/api/v1/users").content(createBody(uniqueEmail(), "Ana", "Pérez", null, "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("al menos un rol")));
        asAdmin(post("/api/v1/users").content(createBody(uniqueEmail(), "Ana", "Pérez", null, "[\"JEFE\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("roles no es válido")));
    }

    @Test
    void editingChangesNamesAndRolesButNeverTheEmail() throws Exception {
        String email = uniqueEmail();
        long id = createdId(email, "[\"TUTOR\"]");

        asAdmin(put("/api/v1/users/" + id).content(
                "{\"email\":\"otro@x.pe\",\"firstName\":\"Ana María\",\"lastNamePat\":\"Pérez\","
                        + "\"lastNameMat\":\"Soto\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Ana María"))
                .andExpect(jsonPath("$.lastNameMat").value("Soto"))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));

        asAdmin(put("/api/v1/users/999999").content(updateBody("A", "B", null, "[\"TUTOR\"]")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el usuario solicitado."));
    }

    @Test
    void permanentAdminCannotBeDeactivatedNorLoseTheAdminRole() throws Exception {
        long id = permanentAdminId();
        asAdmin(get("/api/v1/users/" + id)).andExpect(jsonPath("$.permanentAdmin").value(true));

        asAdmin(delete("/api/v1/users/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("administrador permanente")));
        asAdmin(put("/api/v1/users/" + id).content(updateBody("Antony", "Palacios", "Coronado", "[\"TUTOR\"]")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("rol ADMIN")));

        // keeping ADMIN (and adding another role) is fine
        asAdmin(put("/api/v1/users/" + id).content(
                updateBody("Antony", "Palacios", "Coronado", "[\"ADMIN\",\"TUTOR\"]")))
                .andExpect(status().isOk());
        asAdmin(put("/api/v1/users/" + id).content(updateBody("Antony", "Palacios", "Coronado", "[\"ADMIN\"]")))
                .andExpect(status().isOk());
        assertThat(userRepository.findById(id).orElseThrow().isActive()).isTrue();
    }

    @Test
    void deactivatedUsersAreHiddenByDefaultAndCanBeReactivated() throws Exception {
        String email = uniqueEmail();
        long id = createdId(email, "[\"TUTOR\"]");

        asAdmin(delete("/api/v1/users/" + id)).andExpect(status().isNoContent());

        String activeOnly = asAdmin(get("/api/v1/users")).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(activeOnly, "$[*].email")).doesNotContain(email);
        String all = asAdmin(get("/api/v1/users?includeInactive=true")).andReturn().getResponse()
                .getContentAsString();
        assertThat(JsonPath.<List<String>>read(all, "$[*].email")).contains(email);

        asAdmin(post("/api/v1/users/" + id + "/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        asAdmin(post("/api/v1/users/" + id + "/reactivate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este usuario ya está activo."));
        asAdmin(delete("/api/v1/users/999999")).andExpect(status().isNotFound());
    }

    @Test
    void usersAreListedByLastNamesIgnoringCaseAndAccents() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String[][] people = {
                {"Zoe", "Ñique" + suffix, null},
                {"Beto", "álvarez" + suffix, "Ruiz"},
                {"Ana", "Alvarez" + suffix, "Quispe"},
        };
        for (String[] p : people) {
            asAdmin(post("/api/v1/users").content(createBody(uniqueEmail(), p[0], p[1], p[2], "[\"TUTOR\"]")))
                    .andExpect(status().isCreated());
        }
        String json = asAdmin(get("/api/v1/users")).andReturn().getResponse().getContentAsString();
        List<String> firstNames = JsonPath.read(json, "$[?(@.lastNamePat =~ /.*" + suffix + "/)].firstName");
        assertThat(firstNames).containsExactly("Ana", "Beto", "Zoe");
    }

    @Test
    void onlyAdminsCanUseTheseEndpoints() throws Exception {
        for (String role : new String[] {"TUTOR", "COORDINADOR"}) {
            mockMvc.perform(get("/api/v1/users").with(user("x").roles(role)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/users").with(user("x").roles(role)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(uniqueEmail(), "Ana", "Pérez", null, "[\"TUTOR\"]")))
                    .andExpect(status().isForbidden());
        }
    }
}
