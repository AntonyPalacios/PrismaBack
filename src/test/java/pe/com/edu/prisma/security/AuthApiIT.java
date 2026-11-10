package pe.com.edu.prisma.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.edu.prisma.config.ContainersConfig;
import pe.com.edu.prisma.users.RoleRepository;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.UserRepository;

/**
 * Session endpoints through the real filter chain, with a real JWT cookie
 * (not @WithMockUser) so the cookie filter and the DB re-check run too.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class AuthApiIT {

    private static final String ADMIN_EMAIL = "apalaciosc0109@gmail.com";
    private static final String TUTOR_EMAIL = "silvia.valleg@prisma.edu.pe";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Cookie sessionFor(User user) {
        return new Cookie("prisma_token", jwtService.issueToken(user.getId(), user.getEmail()));
    }

    @Test
    void seededUsersExistWithTheirRoles() {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        User tutor = userRepository.findByEmail(TUTOR_EMAIL).orElseThrow();

        assertThat(admin.isActive()).isTrue();
        assertThat(admin.getRoles()).extracting("name").containsExactly("ADMIN");
        assertThat(tutor.getFullName()).isEqualTo("Silvia Valle Gómez");
        assertThat(tutor.getRoles()).extracting("name").containsExactly("TUTOR");
    }

    @Test
    void meReturnsTheLoggedInUser() throws Exception {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();

        mockMvc.perform(get("/api/v1/auth/me").cookie(sessionFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId()))
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Antony"))
                .andExpect(jsonPath("$.lastName").value("Palacios Coronado"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.photoUrl").doesNotExist());
    }

    @Test
    void apiWithoutSessionAnswers401ProblemDetailsInsteadOfRedirecting() throws Exception {
        for (String path : new String[] {"/api/v1/auth/me", "/api/v1/areas"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().doesNotExist("Location"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value(
                            "Tu sesión expiró o no has iniciado sesión. Vuelve a ingresar."))
                    .andExpect(jsonPath("$.errorId").exists());
        }
    }

    @Test
    void tamperedTokenIsTreatedAsNoSession() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").cookie(new Cookie("prisma_token", "not-a-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatedUserLosesAccessOnTheNextRequest() throws Exception {
        User user = new User();
        user.setEmail("inactive-" + UUID.randomUUID() + "@example.com");
        user.setFirstName("Ina");
        user.setLastNamePat("Activa");
        user.getRoles().add(roleRepository.findByName("TUTOR").orElseThrow());
        user = userRepository.save(user);
        Cookie session = sessionFor(user);

        mockMvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isOk());

        user.setActive(false);
        userRepository.save(user);

        mockMvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutExpiresTheSessionCookie() throws Exception {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();

        mockMvc.perform(post("/api/v1/auth/logout").with(csrf()).cookie(sessionFor(admin)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("prisma_token", 0));
    }

    @Test
    void logoutWithoutSessionIsStillNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutRequiresTheCsrfToken() throws Exception {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();

        mockMvc.perform(post("/api/v1/auth/logout").cookie(sessionFor(admin)))
                .andExpect(status().isForbidden());
    }
}
