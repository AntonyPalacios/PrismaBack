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
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.com.edu.prisma.config.ContainersConfig;

/**
 * Stages of a cycle (REQ-ACADEMIC-02 + agreements of 2026-11-12): list,
 * create, edit and delete, and the cycle rules that depend on its stages.
 * A few tests set a CLOSED stage or cycle with plain SQL to reach that
 * state directly, without walking through every date rule.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class StageApiIT {

    private static final String OUTSIDE_CYCLE = "Las fechas de la etapa deben estar dentro de las fechas de su ciclo: "
            + "no puede empezar antes ni terminar después que el ciclo.";
    private static final String OVERLAP = "Las fechas de esta etapa se cruzan con las de otra etapa del ciclo.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    // plain SQL: soft-deleted rows are invisible to the repositories but must go too
    @BeforeEach
    @AfterEach
    void emptyTables() {
        jdbc.update("DELETE FROM exam");
        jdbc.update("DELETE FROM tutor_assignment");
        jdbc.update("DELETE FROM student_stage");
        jdbc.update("DELETE FROM student");
        jdbc.update("DELETE FROM stage");
        jdbc.update("DELETE FROM cycle");
    }

    private static String body(String name, String startDate, String endDate) {
        return "{\"name\":" + quote(name) + ",\"startDate\":" + quote(startDate)
                + ",\"endDate\":" + quote(endDate) + "}";
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    /** "Today" as the backend sees it (ClockConfig). */
    private static String day(int offsetFromToday) {
        return LocalDate.now(ZoneId.of("America/Lima")).plusDays(offsetFromToday).toString();
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return as("ADMIN", request);
    }

    private ResultActions as(String role, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user("someone").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON));
    }

    private static long idOf(ResultActions created) throws Exception {
        String json = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long cycleId(String name, String startDate, String endDate) throws Exception {
        return idOf(asAdmin(post("/api/v1/cycles").content(body(name, startDate, endDate))));
    }

    private static String stages(long cycleId) {
        return "/api/v1/cycles/" + cycleId + "/stages";
    }

    private ResultActions create(long cycleId, String name, String startDate, String endDate) throws Exception {
        return asAdmin(post(stages(cycleId)).content(body(name, startDate, endDate)));
    }

    private long stageId(long cycleId, String name, String startDate, String endDate) throws Exception {
        return idOf(create(cycleId, name, startDate, endDate));
    }

    private ResultActions edit(long cycleId, long id, String name, String startDate, String endDate)
            throws Exception {
        return asAdmin(put(stages(cycleId) + "/" + id).content(body(name, startDate, endDate)));
    }

    private List<String> currentStageNames(long cycleId) throws Exception {
        String json = asAdmin(get(stages(cycleId))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[?(@.current == true)].name");
    }

    @Test
    void adminCreatesAStageThatIsBornPlanned(CapturedOutput output) throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");

        create(cycle, "  Nivelación  ", "2026-03-01", "2026-04-15")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(stages(cycle) + "/")))
                .andExpect(jsonPath("$.cycleId").value(cycle))
                .andExpect(jsonPath("$.name").value("Nivelación"))
                .andExpect(jsonPath("$.startDate").value("2026-03-01"))
                .andExpect(jsonPath("$.endDate").value("2026-04-15"))
                .andExpect(jsonPath("$.status").value("PLANNED"));

        // one INFO line per write, with the id (logging agreement)
        assertThat(output.getOut()).contains("Stage created: stageId=");
    }

    @Test
    void stagesAreListedByStartDateAndTheEndDateIsOptional() throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");
        stageId(cycle, "Simulacros", "2026-06-01", null);
        stageId(cycle, "Nivelación", "2026-03-01", "2026-04-15");
        long middle = stageId(cycle, "Reforzamiento", "2026-04-16", "2026-05-31");

        String json = asAdmin(get(stages(cycle))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> names = JsonPath.read(json, "$[*].name");
        assertThat(names).containsExactly("Nivelación", "Reforzamiento", "Simulacros");

        asAdmin(get(stages(cycle) + "/" + middle))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Reforzamiento"));
        asAdmin(get(stages(cycle)))
                .andExpect(jsonPath("$[2].endDate").doesNotExist());
    }

    @Test
    void theCurrentStageIsTheOneContainingToday() throws Exception {
        long cycle = cycleId("En curso", day(-60), day(60));
        assertThat(currentStageNames(cycle)).isEmpty();

        stageId(cycle, "Primera", day(-60), day(-31));
        long second = stageId(cycle, "Segunda", day(-30), day(10));
        stageId(cycle, "Tercera", day(11), null);

        assertThat(currentStageNames(cycle)).containsExactly("Segunda");
        asAdmin(get(stages(cycle) + "/" + second)).andExpect(jsonPath("$.current").value(true));
    }

    @Test
    void whenNoStageContainsTodayTheOneThatStartedMostRecentlyIsCurrentWhateverTheStatus() throws Exception {
        long cycle = cycleId("En curso", day(-60), day(-1));
        stageId(cycle, "Primera", day(-60), day(-41));
        long second = stageId(cycle, "Segunda", day(-30), day(-10));

        // today falls in the gap after "Segunda": it is still the current one
        assertThat(currentStageNames(cycle)).containsExactly("Segunda");

        // even when every stage and the cycle itself are closed
        jdbc.update("UPDATE stage SET status = 'CLOSED' WHERE cycle_id = ?", cycle);
        jdbc.update("UPDATE cycle SET status = 'CLOSED' WHERE id = ?", cycle);
        assertThat(currentStageNames(cycle)).containsExactly("Segunda");
        asAdmin(get(stages(cycle) + "/" + second))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.current").value(true));
    }

    @Test
    void noStageIsCurrentWhileNoneHasStarted() throws Exception {
        long cycle = cycleId("Futuro", day(10), day(60));
        stageId(cycle, "Primera", day(10), day(30));

        assertThat(currentStageNames(cycle)).isEmpty();
    }

    @Test
    void aStageMustStayInsideItsCycleDates() throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");

        create(cycle, "Antes", "2026-02-28", "2026-03-15").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(OUTSIDE_CYCLE));
        create(cycle, "Después", "2026-07-01", "2026-08-01").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(OUTSIDE_CYCLE));
        create(cycle, "Fuera", "2026-08-01", null).andExpect(status().isConflict());

        // exactly the cycle's limits are fine, and so is an open-ended stage that starts inside
        long whole = stageId(cycle, "Todo el ciclo", "2026-03-01", "2026-07-31");
        edit(cycle, whole, "Todo el ciclo", "2026-03-01", "2026-05-31").andExpect(status().isOk());
        create(cycle, "Abierta", "2026-06-01", null).andExpect(status().isCreated());
        edit(cycle, whole, "Todo el ciclo", "2026-02-01", "2026-05-31").andExpect(status().isConflict());

        // a cycle without end date only limits the start
        long open = cycleId("2026-II", "2026-08-01", null);
        create(open, "Lejana", "2030-01-01", "2030-06-30").andExpect(status().isCreated());
        create(open, "Antes", "2026-07-31", null).andExpect(status().isConflict());
    }

    @Test
    void stagesOfACycleCannotShareEvenOneDay() throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");
        long first = stageId(cycle, "Primera", "2026-04-01", "2026-04-30");

        create(cycle, "a", "2026-04-10", "2026-04-20").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(OVERLAP));
        create(cycle, "b", "2026-03-01", "2026-05-31").andExpect(status().isConflict());
        create(cycle, "c", "2026-04-30", "2026-05-15").andExpect(status().isConflict());
        create(cycle, "d", "2026-03-15", "2026-04-01").andExpect(status().isConflict());
        // a stage without end date is open-ended: it collides with everything after its start
        create(cycle, "e", "2026-03-15", null).andExpect(status().isConflict());

        // the day after and the day before are fine, and a stage never collides with itself
        create(cycle, "Segunda", "2026-05-01", null).andExpect(status().isCreated());
        create(cycle, "Previa", "2026-03-01", "2026-03-31").andExpect(status().isCreated());
        edit(cycle, first, "Primera", "2026-04-05", "2026-04-25").andExpect(status().isOk());
        create(cycle, "f", "2026-06-01", "2026-06-30").andExpect(status().isConflict());
    }

    @Test
    void namesAreUniqueWithinACycleIgnoringCase() throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");
        long other = cycleId("2026-II", "2026-08-01", "2026-12-15");
        long first = stageId(cycle, "Nivelación", "2026-03-01", "2026-04-15");
        long second = stageId(cycle, "Reforzamiento", "2026-04-16", "2026-05-31");

        create(cycle, "NIVELACIÓN", "2026-06-01", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe una etapa con ese nombre en este ciclo."));
        edit(cycle, second, "nivelación", "2026-04-16", "2026-05-31").andExpect(status().isConflict());

        // a stage keeps its own name when edited, and another cycle can reuse it
        edit(cycle, first, "NIVELACIÓN", "2026-03-01", "2026-04-15").andExpect(status().isOk());
        create(other, "Nivelación", "2026-08-01", "2026-09-15").andExpect(status().isCreated());
    }

    @Test
    void adminEditsNameAndDates(CapturedOutput output) throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");
        long id = stageId(cycle, "Nivelación", "2026-03-01", "2026-04-15");

        edit(cycle, id, "Etapa 1", "2026-03-05", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Etapa 1"))
                .andExpect(jsonPath("$.startDate").value("2026-03-05"))
                .andExpect(jsonPath("$.endDate").doesNotExist())
                .andExpect(jsonPath("$.status").value("PLANNED"));

        assertThat(output.getOut()).contains("Stage updated: stageId=" + id + " cycleId=" + cycle);
    }

    @Test
    void invalidRequestsAnswer400InSpanish() throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");

        create(cycle, "", "2026-03-01", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("necesita un nombre")));
        create(cycle, "x".repeat(151), "2026-03-01", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("demasiado largo")));
        create(cycle, "Etapa", null, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("fecha de inicio")));
        create(cycle, "Etapa", "2026-04-15", "2026-03-01").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("La fecha de fin no puede ser anterior a la fecha de inicio."));

        asAdmin(get(stages(cycle))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknownCycleOrStageAnswers404InSpanish() throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");
        long other = cycleId("2026-II", "2026-08-01", "2026-12-15");
        long id = stageId(cycle, "Nivelación", "2026-03-01", "2026-04-15");

        asAdmin(get(stages(999999))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el ciclo solicitado."));
        create(999999, "Etapa", "2026-03-01", null).andExpect(status().isNotFound());
        asAdmin(get(stages(cycle) + "/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos la etapa solicitada."));

        // a stage is only reachable through its own cycle
        asAdmin(get(stages(other) + "/" + id)).andExpect(status().isNotFound());
        edit(other, id, "Nivelación", "2026-08-01", null).andExpect(status().isNotFound());
        asAdmin(delete(stages(other) + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void inAClosedCycleStagesCanOnlyBeDeleted() throws Exception {
        long cycle = cycleId("2025-II", "2025-08-01", "2025-12-15");
        long id = stageId(cycle, "Nivelación", "2025-08-01", "2025-09-15");
        jdbc.update("UPDATE stage SET status = 'CLOSED' WHERE id = ?", id);
        jdbc.update("UPDATE cycle SET status = 'CLOSED' WHERE id = ?", cycle);
        String closed = "Este ciclo está cerrado: ya no se pueden crear ni editar sus etapas.";

        create(cycle, "Otra", "2025-10-01", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(closed));
        edit(cycle, id, "Renombrada", "2025-08-01", "2025-09-15").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(closed));

        asAdmin(delete(stages(cycle) + "/" + id)).andExpect(status().isNoContent());
    }

    @Test
    void aClosedStageCanNoLongerBeEdited() throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");
        long id = stageId(cycle, "Nivelación", "2026-03-01", "2026-04-15");
        jdbc.update("UPDATE stage SET status = 'CLOSED' WHERE id = ?", id);

        edit(cycle, id, "Renombrada", "2026-03-01", "2026-04-15")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta etapa está cerrada y ya no se puede editar."));
    }

    @Test
    void aDeletedStageDisappearsAndFreesItsNameAndDates(CapturedOutput output) throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");
        long id = stageId(cycle, "Nivelación", "2026-03-01", null);

        asAdmin(delete(stages(cycle) + "/" + id)).andExpect(status().isNoContent());

        asAdmin(get(stages(cycle) + "/" + id)).andExpect(status().isNotFound());
        asAdmin(get(stages(cycle))).andExpect(jsonPath("$.length()").value(0));
        asAdmin(delete(stages(cycle) + "/" + id)).andExpect(status().isNotFound());
        // it was open-ended: it blocked every later stage of the cycle
        create(cycle, "nivelación", "2026-03-01", "2026-04-15").andExpect(status().isCreated());

        // soft delete: the row is still in the table, and the log line is its trace
        assertThat(jdbc.queryForObject("SELECT deleted FROM stage WHERE id = ?", Boolean.class, id)).isTrue();
        assertThat(output.getOut())
                .contains("Stage deleted: stageId=" + id + " cycleId=" + cycle + " name=\"Nivelación\" status=PLANNED");
    }

    @Test
    void aStageWithEnrollmentsOrExamsCannotBeDeleted() throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");
        long enrolled = stageId(cycle, "Con alumnos", "2026-03-01", "2026-04-15");
        long examined = stageId(cycle, "Con exámenes", "2026-04-16", "2026-05-31");
        String hasData = "Esta etapa tiene alumnos matriculados o exámenes, así que no se puede eliminar.";

        jdbc.update("INSERT INTO student (first_name, last_name_pat) VALUES ('Ana', 'Pérez')");
        Long student = jdbc.queryForObject("SELECT max(id) FROM student", Long.class);
        jdbc.update("INSERT INTO student_stage (student_id, stage_id) VALUES (?, ?)", student, enrolled);
        jdbc.update("INSERT INTO exam (stage_id, exam_type_id, name, exam_date) VALUES (?, 1, 'Simulacro 1', '2026-05-02')",
                examined);

        asAdmin(delete(stages(cycle) + "/" + enrolled)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(hasData));
        asAdmin(delete(stages(cycle) + "/" + examined)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(hasData));
        asAdmin(get(stages(cycle))).andExpect(jsonPath("$.length()").value(2));
    }

    // ---- cycle rules that depend on its stages ----

    @Test
    void aCycleCannotBeClosedWhileAnyOfItsStagesIsNotClosed() throws Exception {
        long cycle = cycleId("En curso", day(-30), day(-1));
        long id = stageId(cycle, "Única", day(-30), day(-1));
        asAdmin(patch("/api/v1/cycles/" + cycle + "/status").content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());

        asAdmin(patch("/api/v1/cycles/" + cycle + "/status").content("{\"status\":\"CLOSED\",\"confirm\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Este ciclo tiene etapas sin cerrar. Cierra todas sus etapas antes de cerrar el ciclo."))
                .andExpect(jsonPath("$.requiresConfirmation").doesNotExist());
        asAdmin(get("/api/v1/cycles/" + cycle)).andExpect(jsonPath("$.status").value("ACTIVE"));

        // the system never closes stages on its own; once they are closed, the cycle closes
        jdbc.update("UPDATE stage SET status = 'CLOSED' WHERE id = ?", id);
        asAdmin(patch("/api/v1/cycles/" + cycle + "/status").content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void aCycleCannotChangeItsDatesLeavingAStageOutside() throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");
        stageId(cycle, "Nivelación", "2026-03-10", "2026-04-15");
        stageId(cycle, "Simulacros", "2026-06-01", "2026-07-20");
        String outside = "Hay etapas que quedarían fuera de las nuevas fechas del ciclo. "
                + "Corrige primero las fechas de esas etapas.";

        asAdmin(put("/api/v1/cycles/" + cycle).content(body("2026-I", "2026-03-11", "2026-07-31")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(outside));
        asAdmin(put("/api/v1/cycles/" + cycle).content(body("2026-I", "2026-03-01", "2026-07-19")))
                .andExpect(status().isConflict());
        asAdmin(put("/api/v1/cycles/" + cycle).content(body("2026-I", "2026-03-01", "2026-05-31")))
                .andExpect(status().isConflict());

        // tightening up to the stages' own limits, or dropping the end date, is fine
        asAdmin(put("/api/v1/cycles/" + cycle).content(body("2026-I", "2026-03-10", "2026-07-20")))
                .andExpect(status().isOk());
        asAdmin(put("/api/v1/cycles/" + cycle).content(body("2026-I", "2026-03-10", null)))
                .andExpect(status().isOk());
    }

    @Test
    void aCycleWithStagesCannotBeDeletedUntilTheyAreDeleted() throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");
        long id = stageId(cycle, "Nivelación", "2026-03-01", "2026-04-15");

        asAdmin(delete("/api/v1/cycles/" + cycle)).andExpect(status().isConflict());

        asAdmin(delete(stages(cycle) + "/" + id)).andExpect(status().isNoContent());
        asAdmin(delete("/api/v1/cycles/" + cycle)).andExpect(status().isNoContent());
        // a deleted cycle hides its stages too
        asAdmin(get(stages(cycle))).andExpect(status().isNotFound());
    }

    // ---- status changes: PLANNED → ACTIVE → CLOSED ----

    private long activeCycleId(String name, String startDate, String endDate) throws Exception {
        long id = cycleId(name, startDate, endDate);
        asAdmin(patch("/api/v1/cycles/" + id + "/status").content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        return id;
    }

    private ResultActions changeStatus(long cycleId, long id, String status) throws Exception {
        return asAdmin(patch(stages(cycleId) + "/" + id + "/status").content("{\"status\":" + quote(status) + "}"));
    }

    @Test
    void aStageIsActivatedThenClosedAndThenItsCycleCanBeClosed(CapturedOutput output) throws Exception {
        long cycle = activeCycleId("En curso", day(-30), day(0));
        long id = stageId(cycle, "Única", day(-30), day(-1));

        changeStatus(cycle, id, "ACTIVE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        changeStatus(cycle, id, "CLOSED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.endDate").value(day(-1)));

        // every stage is closed: now the cycle closes, end to end through the API
        asAdmin(patch("/api/v1/cycles/" + cycle + "/status").content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        assertThat(output.getOut())
                .contains("Stage status changed: stageId=" + id + " cycleId=" + cycle + " from=PLANNED to=ACTIVE")
                .contains("Stage status changed: stageId=" + id + " cycleId=" + cycle + " from=ACTIVE to=CLOSED");
    }

    @Test
    void aStageIsOnlyActivatedInAnActiveCycle() throws Exception {
        long cycle = cycleId("Planificado", day(-30), day(30));
        long id = stageId(cycle, "Primera", day(-30), day(-1));

        changeStatus(cycle, id, "ACTIVE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Para activar una etapa, su ciclo tiene que estar activo. Activa primero el ciclo."));

        asAdmin(patch("/api/v1/cycles/" + cycle + "/status").content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        changeStatus(cycle, id, "ACTIVE").andExpect(status().isOk());
    }

    @Test
    void aStageWithAFutureStartDateCannotBeActivated() throws Exception {
        long cycle = activeCycleId("En curso", day(-30), day(30));
        long id = stageId(cycle, "Futura", day(1), day(20));

        changeStatus(cycle, id, "ACTIVE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("su fecha de inicio es futura")));
        asAdmin(get(stages(cycle) + "/" + id)).andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    void onlyOneStageOfACycleCanBeActive() throws Exception {
        long cycle = activeCycleId("En curso", day(-30), day(30));
        long first = stageId(cycle, "Primera", day(-30), day(-10));
        long second = stageId(cycle, "Segunda", day(-9), day(5));
        changeStatus(cycle, first, "ACTIVE").andExpect(status().isOk());

        changeStatus(cycle, second, "ACTIVE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Ya hay otra etapa activa en este ciclo. Ciérrala antes de activar esta."));

        changeStatus(cycle, first, "CLOSED").andExpect(status().isOk());
        changeStatus(cycle, second, "ACTIVE").andExpect(status().isOk());
    }

    @Test
    void stageStatusOnlyMovesForwardOneStepAtATime() throws Exception {
        String forwardOnly = "Una etapa solo puede avanzar: de planificada a activa y de activa a cerrada.";
        long cycle = activeCycleId("En curso", day(-30), day(30));
        long id = stageId(cycle, "Primera", day(-30), day(-1));

        changeStatus(cycle, id, "CLOSED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(forwardOnly));
        changeStatus(cycle, id, "PLANNED").andExpect(status().isConflict());

        changeStatus(cycle, id, "ACTIVE").andExpect(status().isOk());
        changeStatus(cycle, id, "ACTIVE").andExpect(status().isConflict());
        changeStatus(cycle, id, "PLANNED").andExpect(status().isConflict());

        changeStatus(cycle, id, "CLOSED").andExpect(status().isOk());
        changeStatus(cycle, id, "ACTIVE").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(forwardOnly));
        changeStatus(cycle, id, "CLOSED").andExpect(status().isConflict());
    }

    @Test
    void aStageWithoutEndDateCannotBeClosed() throws Exception {
        long cycle = activeCycleId("En curso", day(-30), day(30));
        long id = stageId(cycle, "Abierta", day(-30), null);
        changeStatus(cycle, id, "ACTIVE").andExpect(status().isOk());

        changeStatus(cycle, id, "CLOSED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta etapa no tiene fecha de fin. Defínela antes de cerrarla."));

        // once the admin sets it, the stage closes
        edit(cycle, id, "Abierta", day(-30), day(-1)).andExpect(status().isOk());
        changeStatus(cycle, id, "CLOSED").andExpect(status().isOk());
    }

    @Test
    void aStageIsNeverClosedBeforeItsEndDateAndTheSystemDoesNotChangeIt() throws Exception {
        long cycle = activeCycleId("En curso", day(-30), day(30));
        long id = stageId(cycle, "En marcha", day(-30), day(10));
        changeStatus(cycle, id, "ACTIVE").andExpect(status().isOk());

        // unlike a cycle there is no confirmation: the answer is a plain 409
        changeStatus(cycle, id, "CLOSED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("corrige primero su fecha de fin")))
                .andExpect(jsonPath("$.requiresConfirmation").doesNotExist());
        asAdmin(get(stages(cycle) + "/" + id))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.endDate").value(day(10)));

        // the user corrects the end date to today, and then it closes
        edit(cycle, id, "En marcha", day(-30), day(0)).andExpect(status().isOk());
        changeStatus(cycle, id, "CLOSED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDate").value(day(0)));
    }

    @Test
    void aStageThatHasNotStartedCannotBeClosed() throws Exception {
        long cycle = activeCycleId("En curso", day(-30), day(30));
        long id = stageId(cycle, "Movida", day(-1), day(20));
        changeStatus(cycle, id, "ACTIVE").andExpect(status().isOk());
        // an active stage can still be edited: its start is pushed into the future
        edit(cycle, id, "Movida", day(5), day(20)).andExpect(status().isOk());

        changeStatus(cycle, id, "CLOSED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Esta etapa aún no empieza, así que no se puede cerrar. Corrige su fecha de inicio."));
    }

    @Test
    void invalidStageStatusRequestsAnswer400Or404() throws Exception {
        long cycle = activeCycleId("En curso", day(-30), day(30));
        long other = cycleId("Siguiente", day(31), day(60));
        long id = stageId(cycle, "Primera", day(-30), day(-1));

        asAdmin(patch(stages(cycle) + "/" + id + "/status").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indica el nuevo estado de la etapa."));
        changeStatus(cycle, id, "ARCHIVED").andExpect(status().isBadRequest());

        changeStatus(cycle, 999999, "ACTIVE").andExpect(status().isNotFound());
        changeStatus(other, id, "ACTIVE").andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TUTOR", "COORDINADOR"})
    void onlyAdminsWriteButEveryRoleReads(String role) throws Exception {
        long cycle = cycleId("2026-I", "2026-03-01", "2026-07-31");
        long id = stageId(cycle, "Nivelación", "2026-03-01", "2026-04-15");

        as(role, get(stages(cycle))).andExpect(status().isOk());
        as(role, get(stages(cycle) + "/" + id)).andExpect(status().isOk());
        as(role, post(stages(cycle)).content(body("Otra", "2026-05-01", null))).andExpect(status().isForbidden());
        as(role, put(stages(cycle) + "/" + id).content(body("x", "2026-03-01", null)))
                .andExpect(status().isForbidden());
        as(role, delete(stages(cycle) + "/" + id)).andExpect(status().isForbidden());
        as(role, patch(stages(cycle) + "/" + id + "/status").content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void readingRequiresASession() throws Exception {
        mockMvc.perform(get(stages(1))).andExpect(status().isUnauthorized());
    }
}
