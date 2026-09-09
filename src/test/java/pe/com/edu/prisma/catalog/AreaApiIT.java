package pe.com.edu.prisma.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.edu.prisma.config.ContainersConfig;

/**
 * End-to-end through the real stack: HTTP -> controller -> service ->
 * JPA -> Postgres (Testcontainers) -> Flyway-migrated schema. This is the
 * pattern to copy for every other module's "happy path + one rule
 * violation + one authorization check" test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class AreaApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "TUTOR")
    void anyAuthenticatedUserCanListAreas() throws Exception {
        mockMvc.perform(get("/api/v1/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @WithMockUser(roles = "TUTOR")
    void tutorCannotCreateAnArea() throws Exception {
        mockMvc.perform(post("/api/v1/areas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ciencias Sociales\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("No tienes permiso para realizar esta acción."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreatingAreaWithoutNameGetsAFriendlySpanishMessage() throws Exception {
        mockMvc.perform(post("/api/v1/areas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("necesita un nombre")))
                .andExpect(jsonPath("$.errorId").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateAnArea() throws Exception {
        mockMvc.perform(post("/api/v1/areas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ciencias Sociales\",\"abbreviation\":\"CCSS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ciencias Sociales"))
                .andExpect(jsonPath("$.active").value(true));
    }
}
