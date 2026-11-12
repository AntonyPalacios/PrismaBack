package pe.com.edu.prisma.academic;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.com.edu.prisma.config.ContainersConfig;

/**
 * Academic cycles (REQ-ACADEMIC-01): list, create and edit. No cycle is
 * seeded and nothing else uses them yet, so every test starts from an
 * empty table and is free to pick its own dates.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class CycleApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CycleRepository cycleRepository;

    @BeforeEach
    void emptyTable() {
        cycleRepository.deleteAll();
    }

    private static String body(String name, String startDate, String endDate) {
        return "{\"name\":" + quote(name) + ",\"startDate\":" + quote(startDate)
                + ",\"endDate\":" + quote(endDate) + "}";
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return as("ADMIN", request);
    }

    private ResultActions as(String role, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user("someone").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions create(String name, String startDate, String endDate) throws Exception {
        return asAdmin(post("/api/v1/cycles").content(body(name, startDate, endDate)));
    }

    private long createdId(String name, String startDate, String endDate) throws Exception {
        String json = create(name, startDate, endDate).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void adminCreatesACycleThatIsBornPlanned(CapturedOutput output) throws Exception {
        create("  2026-I  ", "2026-03-01", "2026-07-31")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/cycles/")))
                .andExpect(jsonPath("$.name").value("2026-I"))
                .andExpect(jsonPath("$.startDate").value("2026-03-01"))
                .andExpect(jsonPath("$.endDate").value("2026-07-31"))
                .andExpect(jsonPath("$.status").value("PLANNED"));

        // one INFO line per write, with the id (logging agreement)
        assertThat(output.getOut()).contains("Cycle created: cycleId=");
    }

    @Test
    void theEndDateIsOptional() throws Exception {
        long id = createdId("2026-I", "2026-03-01", null);

        asAdmin(get("/api/v1/cycles/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDate").doesNotExist());
    }

    @Test
    void cyclesAreListedMostRecentFirst() throws Exception {
        createdId("2025-II", "2025-08-01", "2025-12-15");
        createdId("2026-II", "2026-08-01", null);
        createdId("2026-I", "2026-03-01", "2026-07-31");

        String json = asAdmin(get("/api/v1/cycles")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> names = JsonPath.read(json, "$[*].name");
        assertThat(names).containsExactly("2026-II", "2026-I", "2025-II");
    }

    @Test
    void adminEditsNameAndDates(CapturedOutput output) throws Exception {
        long id = createdId("2026-I", "2026-03-01", "2026-07-31");

        asAdmin(put("/api/v1/cycles/" + id).content(body("Verano 2026", "2026-01-05", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Verano 2026"))
                .andExpect(jsonPath("$.startDate").value("2026-01-05"))
                .andExpect(jsonPath("$.endDate").doesNotExist())
                .andExpect(jsonPath("$.status").value("PLANNED"));

        assertThat(output.getOut()).contains("Cycle updated: cycleId=" + id);
    }

    @Test
    void aClosedCycleCanNoLongerBeEdited() throws Exception {
        long id = createdId("2025-II", "2025-08-01", "2025-12-15");
        Cycle cycle = cycleRepository.findById(id).orElseThrow();
        cycle.setStatus(CycleStatus.CLOSED);
        cycleRepository.save(cycle);

        asAdmin(put("/api/v1/cycles/" + id).content(body("2025-II bis", "2025-08-01", "2025-12-15")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este ciclo está cerrado y ya no se puede editar."));
    }

    @Test
    void invalidRequestsAnswer400InSpanish() throws Exception {
        create("", "2026-03-01", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("necesita un nombre")));
        create("x".repeat(151), "2026-03-01", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("demasiado largo")));
        create("2026-I", null, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("fecha de inicio")));
        create("2026-I", "2026-07-31", "2026-03-01").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("La fecha de fin no puede ser anterior a la fecha de inicio."));

        assertThat(cycleRepository.count()).isZero();
    }

    @Test
    void aCycleCanStartAndEndOnTheSameDay() throws Exception {
        create("Un día", "2026-03-01", "2026-03-01").andExpect(status().isCreated());
    }

    @Test
    void namesAreUniqueIgnoringCase() throws Exception {
        long first = createdId("2026-I", "2026-03-01", "2026-07-31");
        long second = createdId("2026-II", "2026-08-01", "2026-12-15");

        create("2026-i", "2027-03-01", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un ciclo con ese nombre."));
        asAdmin(put("/api/v1/cycles/" + second).content(body("2026-I", "2026-08-01", "2026-12-15")))
                .andExpect(status().isConflict());

        // a cycle keeps its own name when edited
        asAdmin(put("/api/v1/cycles/" + first).content(body("2026-i", "2026-03-01", "2026-07-31")))
                .andExpect(status().isOk());
    }

    @Test
    void cyclesCannotShareEvenOneDay() throws Exception {
        createdId("2026-I", "2026-03-01", "2026-07-31");
        String overlap = "Las fechas de este ciclo se cruzan con las de otro ciclo.";

        // inside, around, and touching either end
        create("a", "2026-04-01", "2026-05-01").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(overlap));
        create("b", "2026-01-01", "2026-12-31").andExpect(status().isConflict());
        create("c", "2026-07-31", "2026-12-15").andExpect(status().isConflict());
        create("d", "2026-01-01", "2026-03-01").andExpect(status().isConflict());

        // the day after and the day before are fine
        create("2026-II", "2026-08-01", "2026-12-15").andExpect(status().isCreated());
        create("Verano 2026", "2026-01-05", "2026-02-28").andExpect(status().isCreated());
    }

    @Test
    void aCycleWithoutEndDateIsOpenEnded() throws Exception {
        createdId("2026-I", "2026-03-01", null);
        long earlier = createdId("2025-II", "2025-08-01", "2025-12-15");

        // anything from its start on collides with the open-ended cycle
        create("2027-I", "2027-03-01", "2027-07-31").andExpect(status().isConflict());
        // and a new open-ended cycle collides with everything after its own start
        create("2025-I", "2025-03-01", null).andExpect(status().isConflict());
        asAdmin(put("/api/v1/cycles/" + earlier).content(body("2025-II", "2025-08-01", null)))
                .andExpect(status().isConflict());

        create("2025-I", "2025-03-01", "2025-07-31").andExpect(status().isCreated());
    }

    @Test
    void editingACycleDoesNotCollideWithItself() throws Exception {
        long id = createdId("2026-I", "2026-03-01", "2026-07-31");

        asAdmin(put("/api/v1/cycles/" + id).content(body("2026-I", "2026-03-15", "2026-08-15")))
                .andExpect(status().isOk());
    }

    @Test
    void unknownCycleAnswers404InSpanish() throws Exception {
        asAdmin(get("/api/v1/cycles/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el ciclo solicitado."));
        asAdmin(put("/api/v1/cycles/999999").content(body("2026-I", "2026-03-01", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cyclesCannotBeDeleted() throws Exception {
        long id = createdId("2026-I", "2026-03-01", null);

        asAdmin(delete("/api/v1/cycles/" + id)).andExpect(status().isMethodNotAllowed());
        assertThat(cycleRepository.existsById(id)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TUTOR", "COORDINADOR"})
    void onlyAdminsWriteButEveryRoleReads(String role) throws Exception {
        long id = createdId("2026-I", "2026-03-01", null);

        as(role, get("/api/v1/cycles")).andExpect(status().isOk());
        as(role, get("/api/v1/cycles/" + id)).andExpect(status().isOk());
        as(role, post("/api/v1/cycles").content(body("2027-I", "2027-03-01", null)))
                .andExpect(status().isForbidden());
        as(role, put("/api/v1/cycles/" + id).content(body("x", "2026-03-01", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void readingRequiresASession() throws Exception {
        mockMvc.perform(get("/api/v1/cycles")).andExpect(status().isUnauthorized());
    }
}
