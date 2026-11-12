package pe.com.edu.prisma.academic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneId;
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

    // ---- status changes: PLANNED → ACTIVE → CLOSED ----

    /** "Today" as the backend sees it (ClockConfig). */
    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("America/Lima"));
    }

    private static String day(int offsetFromToday) {
        return today().plusDays(offsetFromToday).toString();
    }

    private ResultActions changeStatus(long id, String status, Boolean confirm) throws Exception {
        String body = "{\"status\":" + quote(status) + (confirm == null ? "" : ",\"confirm\":" + confirm) + "}";
        return asAdmin(patch("/api/v1/cycles/" + id + "/status").content(body));
    }

    private long activeId(String name, String startDate, String endDate) throws Exception {
        long id = createdId(name, startDate, endDate);
        changeStatus(id, "ACTIVE", null).andExpect(status().isOk());
        return id;
    }

    @Test
    void aStartedCycleIsActivatedAndThenClosedKeepingItsEndDate(CapturedOutput output) throws Exception {
        long id = createdId("Pasado", day(-30), day(-1));

        changeStatus(id, "ACTIVE", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        // the end date already passed: no confirmation needed, and it stays as it is
        changeStatus(id, "CLOSED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.endDate").value(day(-1)));

        assertThat(output.getOut())
                .contains("Cycle status changed: cycleId=" + id + " from=PLANNED to=ACTIVE")
                .contains("Cycle status changed: cycleId=" + id + " from=ACTIVE to=CLOSED");
    }

    @Test
    void aCycleStartingTodayCanBeActivatedAndClosedOnItsLastDay() throws Exception {
        long id = createdId("Hoy", day(0), day(0));

        changeStatus(id, "ACTIVE", null).andExpect(status().isOk());
        changeStatus(id, "CLOSED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDate").value(day(0)));
    }

    @Test
    void aCycleWithAFutureStartDateCannotBeActivated() throws Exception {
        long id = createdId("Futuro", day(1), null);

        changeStatus(id, "ACTIVE", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("su fecha de inicio es futura")))
                .andExpect(jsonPath("$.requiresConfirmation").doesNotExist());
        asAdmin(get("/api/v1/cycles/" + id)).andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    void onlyOneCycleCanBeActive() throws Exception {
        activeId("Primero", day(-60), day(-31));
        long second = createdId("Segundo", day(-30), null);

        changeStatus(second, "ACTIVE", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya hay otro ciclo activo. Ciérralo antes de activar este."));
    }

    @Test
    void statusOnlyMovesForwardOneStepAtATime() throws Exception {
        String forwardOnly = "Un ciclo solo puede avanzar: de planificado a activo y de activo a cerrado.";
        long id = createdId("Ciclo", day(-30), day(-1));

        changeStatus(id, "CLOSED", true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(forwardOnly));
        changeStatus(id, "PLANNED", null).andExpect(status().isConflict());

        changeStatus(id, "ACTIVE", null).andExpect(status().isOk());
        changeStatus(id, "ACTIVE", null).andExpect(status().isConflict());
        changeStatus(id, "PLANNED", null).andExpect(status().isConflict());

        changeStatus(id, "CLOSED", null).andExpect(status().isOk());
        changeStatus(id, "ACTIVE", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(forwardOnly));
        changeStatus(id, "CLOSED", null).andExpect(status().isConflict());
    }

    @Test
    void aCycleWithoutEndDateCannotBeClosedEvenIfConfirmed() throws Exception {
        long id = activeId("Sin fin", day(-10), null);

        changeStatus(id, "CLOSED", true)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("no tiene fecha de fin")))
                .andExpect(jsonPath("$.detail").value(containsString("el siguiente ciclo solo podrá empezar después")))
                .andExpect(jsonPath("$.requiresConfirmation").doesNotExist());
        asAdmin(get("/api/v1/cycles/" + id)).andExpect(jsonPath("$.status").value("ACTIVE"));

        // once the admin sets the end date, it closes
        asAdmin(put("/api/v1/cycles/" + id).content(body("Sin fin", day(-10), day(-1)))).andExpect(status().isOk());
        changeStatus(id, "CLOSED", null).andExpect(status().isOk());
    }

    @Test
    void closingBeforeTheEndDateNeedsConfirmationAndMovesTheEndDateToToday() throws Exception {
        long id = activeId("En curso", day(-10), day(10));

        // without confirmation nothing changes, and the client is told to ask
        changeStatus(id, "CLOSED", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.requiresConfirmation").value(true))
                .andExpect(jsonPath("$.detail").value(containsString("La fecha de fin pasará a ser hoy")));
        changeStatus(id, "CLOSED", false).andExpect(status().isConflict());
        asAdmin(get("/api/v1/cycles/" + id))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.endDate").value(day(10)));

        changeStatus(id, "CLOSED", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.endDate").value(day(0)));

        // the days it gave up are free for the next cycle
        create("Siguiente", day(1), null).andExpect(status().isCreated());
    }

    @Test
    void aCycleThatHasNotStartedCannotBeClosed() throws Exception {
        long id = activeId("Movido", day(-1), day(30));
        // an active cycle can still be edited: its start is pushed into the future
        asAdmin(put("/api/v1/cycles/" + id).content(body("Movido", day(5), day(30)))).andExpect(status().isOk());

        changeStatus(id, "CLOSED", true)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Este ciclo aún no empieza, así que no se puede cerrar. Corrige su fecha de inicio."));
    }

    @Test
    void invalidStatusRequestsAnswer400Or404() throws Exception {
        long id = createdId("Ciclo", day(-30), day(-1));

        asAdmin(patch("/api/v1/cycles/" + id + "/status").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indica el nuevo estado del ciclo."));
        // a value that can't even be read is a 400 too, never a 500
        changeStatus(id, "ARCHIVED", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("No pudimos leer los datos enviados")));
        create("Fecha rota", "no-es-fecha", null).andExpect(status().isBadRequest());

        changeStatus(999999, "ACTIVE", null).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TUTOR", "COORDINADOR"})
    void onlyAdminsWriteButEveryRoleReads(String role) throws Exception {
        long id = createdId("2026-I", "2026-03-01", null);
        as(role, patch("/api/v1/cycles/" + id + "/status").content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden());

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
