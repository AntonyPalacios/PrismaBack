package pe.com.edu.prisma.catalog;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.com.edu.prisma.config.ContainersConfig;

/**
 * Areas and courses are fixed seed data (V2 migration): the API only reads
 * them, in A–Z order, for every role — and offers no way to change them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class CatalogApiIT {

    private static final String WRITES_DISABLED =
            "Esta operación no está disponible.";

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "TUTOR", "COORDINADOR"})
    void everyRoleListsTheTwoAreasAlphabetically(String role) throws Exception {
        mockMvc.perform(get("/api/v1/areas").with(user("u").roles(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", contains("Lectura", "Matemáticas")));
    }

    @Test
    @WithMockUser(roles = "TUTOR")
    void coursesAreListedAlphabeticallyIgnoringAccents() throws Exception {
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                // "Álgebra" sorts as "Algebra": first, not after "Trigonometría"
                .andExpect(jsonPath("$[*].name", contains(
                        "Álgebra", "Estadística", "Geometría", "Lectura", "Números y Operaciones",
                        "Trigonometría")))
                .andExpect(jsonPath("$[0].areaName").value("Matemáticas"))
                .andExpect(jsonPath("$[0].displayLabel").value("Álgebra (ALG)"));
    }

    @Test
    @WithMockUser(roles = "TUTOR")
    void coursesCanBeFilteredByArea() throws Exception {
        mockMvc.perform(get("/api/v1/courses").param("areaId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].areaName", everyItem(is("Matemáticas"))));
    }

    @Test
    @WithMockUser(roles = "TUTOR")
    void areaAndCourseCanBeViewedById() throws Exception {
        mockMvc.perform(get("/api/v1/areas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lectura"));
        mockMvc.perform(get("/api/v1/courses/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Álgebra"));
    }

    @Test
    @WithMockUser(roles = "TUTOR")
    void unknownIdsAnswer404InSpanish() throws Exception {
        mockMvc.perform(get("/api/v1/areas/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el área solicitada."));
        mockMvc.perform(get("/api/v1/courses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el curso solicitado."));
    }

    @Test
    void readingRequiresASession() throws Exception {
        mockMvc.perform(get("/api/v1/areas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/courses")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void areasAndCoursesCannotBeChangedEvenByAnAdmin() throws Exception {
        String body = "{\"name\":\"Ciencias Sociales\",\"areaId\":1}";
        MockHttpServletRequestBuilder[] writes = {
            post("/api/v1/areas"), put("/api/v1/areas/1"), delete("/api/v1/areas/1"),
            post("/api/v1/courses"), put("/api/v1/courses/1"), delete("/api/v1/courses/1"),
        };
        for (MockHttpServletRequestBuilder write : writes) {
            mockMvc.perform(write.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.detail").value(WRITES_DISABLED));
        }
    }
}
