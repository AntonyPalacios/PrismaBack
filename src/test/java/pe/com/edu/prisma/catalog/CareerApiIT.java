package pe.com.edu.prisma.catalog;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * Careers: ADMIN creates, edits (incl. exam type), deactivates and
 * reactivates; everyone authenticated reads. Seed (V2): Ciencias(1),
 * Letras(2), Arquitectura(3), Biomédica(1), Derecho(2), Artes(2) — exam type
 * ids 1 Ciencias, 2 Letras, 3 Arquitectura.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class CareerApiIT {

    @Autowired
    private MockMvc mockMvc;

    private static String uniqueName() {
        return "Carrera " + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String body(String name, String abbreviation, Object examTypeId) {
        String abbr = abbreviation == null ? "null" : "\"" + abbreviation + "\"";
        return "{\"name\":\"" + name + "\",\"abbreviation\":" + abbr + ",\"examTypeId\":" + examTypeId + "}";
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions create(String name, String abbreviation, Object examTypeId) throws Exception {
        return asAdmin(post("/api/v1/careers").content(body(name, abbreviation, examTypeId)));
    }

    private long createdId(String name, long examTypeId) throws Exception {
        String json = create(name, null, examTypeId).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private ResultActions read(String path) throws Exception {
        return mockMvc.perform(get(path).with(user("tutor").roles("TUTOR")));
    }

    @Test
    void seededCareersAreListedAlphabeticallyWithTheirExamType() throws Exception {
        String json = read("/api/v1/careers").andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        List<String> names = JsonPath.read(json, "$[*].name");

        // other tests add careers to the same DB: check the seeded ones keep A–Z order among themselves
        assertThat(names.stream().filter(n -> !n.startsWith("Carrera ")).toList())
                .containsSubsequence("Arquitectura", "Artes", "Biomédica", "Ciencias", "Derecho", "Letras");
        read("/api/v1/careers/4")
                .andExpect(jsonPath("$.name").value("Biomédica"))
                .andExpect(jsonPath("$.abbreviation").value("B"))
                .andExpect(jsonPath("$.examTypeId").value(1))
                .andExpect(jsonPath("$.examTypeName").value("Ciencias"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void unknownCareerAnswers404InSpanish() throws Exception {
        read("/api/v1/careers/999999")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos la carrera solicitada."));
    }

    @Test
    void adminCreatesACareerSharingAnExistingExamType() throws Exception {
        String name = uniqueName();
        create("  " + name + "  ", " ING ", 1)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/careers/")))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.abbreviation").value("ING"))
                .andExpect(jsonPath("$.examTypeName").value("Ciencias"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void editingCanChangeNameAbbreviationAndExamType() throws Exception {
        long id = createdId(uniqueName(), 1);
        String renamed = uniqueName();

        asAdmin(put("/api/v1/careers/" + id).content(body(renamed, "", 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed))
                .andExpect(jsonPath("$.abbreviation").doesNotExist())
                .andExpect(jsonPath("$.examTypeName").value("Arquitectura"));

        asAdmin(put("/api/v1/careers/999999").content(body(uniqueName(), null, 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestsAnswer400InSpanish() throws Exception {
        create("", null, 1).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("necesita un nombre")));
        create("x".repeat(101), null, 1).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("demasiado largo")));
        create(uniqueName(), "x".repeat(21), 1).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("abreviatura es demasiado larga")));
        create(uniqueName(), null, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("tipo de examen")));
    }

    @Test
    void unknownExamTypeAnswers409() throws Exception {
        create(uniqueName(), null, 999)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El tipo de examen seleccionado no existe."));
    }

    @Test
    void namesAreUniqueIgnoringCase() throws Exception {
        create("biomédica", null, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe una carrera con ese nombre."));

        String name = uniqueName();
        long id = createdId(name, 2);
        asAdmin(put("/api/v1/careers/" + id).content(body("DERECHO", null, 2))).andExpect(status().isConflict());
        asAdmin(put("/api/v1/careers/" + id).content(body(name.toUpperCase(), null, 2)))
                .andExpect(status().isOk());
    }

    @Test
    void deactivatingKeepsTheCareerListedAndReactivatingBringsItBack() throws Exception {
        long id = createdId(uniqueName(), 2);

        asAdmin(delete("/api/v1/careers/" + id)).andExpect(status().isNoContent());
        read("/api/v1/careers/" + id).andExpect(jsonPath("$.active").value(false));
        String list = read("/api/v1/careers").andReturn().getResponse().getContentAsString();
        assertThat((List<Object>) JsonPath.read(list, "$[?(@.id == " + id + ")]")).hasSize(1);

        asAdmin(post("/api/v1/careers/" + id + "/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        // reactivating an active career is harmless
        asAdmin(post("/api/v1/careers/" + id + "/reactivate")).andExpect(status().isOk());

        asAdmin(delete("/api/v1/careers/999999")).andExpect(status().isNotFound());
        asAdmin(post("/api/v1/careers/999999/reactivate")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TUTOR", "COORDINADOR"})
    void onlyAdminsWriteButEveryRoleReads(String role) throws Exception {
        mockMvc.perform(get("/api/v1/careers").with(user("u").roles(role))).andExpect(status().isOk());
        MockHttpServletRequestBuilder[] writes = {
            post("/api/v1/careers"), put("/api/v1/careers/1"), delete("/api/v1/careers/1"),
            post("/api/v1/careers/1/reactivate"),
        };
        for (MockHttpServletRequestBuilder write : writes) {
            mockMvc.perform(write.with(user("u").roles(role)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(body(uniqueName(), null, 1)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void readingRequiresASession() throws Exception {
        mockMvc.perform(get("/api/v1/careers")).andExpect(status().isUnauthorized());
    }
}
