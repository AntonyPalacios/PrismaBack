package pe.com.edu.prisma.exams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
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
 * Mock exams of a stage and their exam types (REQ-EXAMS-01, agreements of
 * 2026-11-15). Cycles and stages are inserted straight into the database:
 * an active 2026-I with its "Regular" stage (March to June) and a closed
 * 2025-II with a stage of the same name. Exam types come from the seed
 * (1 Ciencias, 2 Letras, 3 Arquitectura).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class MockExamApiIT {

    private static final String MOCK_EXAMS = "/api/v1/mock-exams";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long active;
    private long regular;
    private long closedRegular;

    @BeforeEach
    void seed() {
        emptyTables();
        active = cycle("2026-I", "2026-01-01", "2026-12-31", "ACTIVE");
        regular = stage(active, "Regular", "2026-03-01", "2026-06-30");
        long closed = cycle("2025-II", "2025-07-01", "2025-12-31", "CLOSED");
        closedRegular = stage(closed, "Regular", "2025-08-01", "2025-11-30");
    }

    @AfterEach
    void emptyTables() {
        jdbc.update("DELETE FROM exam_course_result");
        jdbc.update("DELETE FROM exam_result");
        jdbc.update("DELETE FROM exam");
        jdbc.update("DELETE FROM mock_exam");
        jdbc.update("DELETE FROM tutor_assignment");
        jdbc.update("DELETE FROM student_stage");
        jdbc.update("DELETE FROM student");
        jdbc.update("DELETE FROM stage");
        jdbc.update("DELETE FROM cycle");
    }

    private long cycle(String name, String start, String end, String status) {
        return jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) "
                + "VALUES (?, ?::date, ?::date, ?) RETURNING id", Long.class, name, start, end, status);
    }

    private long stage(long cycle, String name, String start, String end) {
        return jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date) "
                + "VALUES (?, ?, ?::date, ?::date) RETURNING id", Long.class, cycle, name, start, end);
    }

    /** One result for the exam (the import is a later slice): it can no longer be removed or deleted. */
    private void giveResults(long exam) {
        long student = jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat) VALUES ('Lucía', 'Quispe') "
                + "RETURNING id", Long.class);
        long enrollment = jdbc.queryForObject("INSERT INTO student_stage (student_id, stage_id) VALUES (?, ?) RETURNING id",
                Long.class, student, regular);
        jdbc.update("INSERT INTO exam_result (exam_id, student_stage_id, total_score) VALUES (?, ?, 1000)", exam, enrollment);
    }

    private static String createBody(Long stageId, String name, String date, String typeIds) {
        return "{\"stageId\":" + stageId + ",\"name\":" + (name == null ? "null" : "\"" + name + "\"")
                + ",\"examDate\":" + (date == null ? "null" : "\"" + date + "\"") + ",\"examTypeIds\":" + typeIds + "}";
    }

    private static String updateBody(String name, String date) {
        return "{\"name\":\"" + name + "\",\"examDate\":\"" + date + "\"}";
    }

    private ResultActions as(String role, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user("someone").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions create(String body) throws Exception {
        return as("ADMIN", post(MOCK_EXAMS).content(body));
    }

    /** Creates a mock exam in "Regular" and answers its JSON. */
    private String created(String name, String date, String typeIds) throws Exception {
        return create(createBody(regular, name, date, typeIds)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    /** The id of the mock exam's exam of that type. */
    private static long examOf(String json, String typeName) {
        List<Integer> ids = JsonPath.read(json, "$.exams[?(@.examTypeName == '" + typeName + "')].id");
        return ids.get(0);
    }

    private String list(String role) throws Exception {
        return as(role, get(MOCK_EXAMS).param("stageId", String.valueOf(regular))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void aMockExamIsCreatedWithOneExamPerType(CapturedOutput output) throws Exception {
        String json = create(createBody(regular, " Regular 1 ", "2026-03-14", "[3, 1, 2]"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(MOCK_EXAMS + "/")))
                .andExpect(jsonPath("$.name").value("Regular 1"))
                .andExpect(jsonPath("$.stageId").value(regular))
                .andExpect(jsonPath("$.examDate").value("2026-03-14"))
                .andExpect(jsonPath("$.hasResults").value(false))
                // catalog order, whatever order they were sent in
                .andExpect(jsonPath("$.exams[*].examTypeName").value(contains("Ciencias", "Letras", "Arquitectura")))
                .andExpect(jsonPath("$.exams[0].hasResults").value(false))
                .andReturn().getResponse().getContentAsString();

        assertThat(output.getOut()).contains("Mock exam created: mockExamId=" + id(json) + " stageId=" + regular);
    }

    @Test
    void mockExamsAreListedNewestFirst() throws Exception {
        created("Medición", "2026-03-02", "[1, 2]");
        created("Regular 1", "2026-03-14", "[1]");

        String json = list("TUTOR");
        assertThat(JsonPath.<List<String>>read(json, "$[*].name")).containsExactly("Regular 1", "Medición");
        assertThat(JsonPath.<List<String>>read(json, "$[1].exams[*].examTypeName")).containsExactly("Ciencias", "Letras");
    }

    @Test
    void theSameTypeSentTwiceIsOneExam() throws Exception {
        assertThat(JsonPath.<List<Object>>read(created("Regular 1", "2026-03-14", "[1, 1]"), "$.exams")).hasSize(1);
    }

    @Test
    void renamingOrMovingTheDateAppliesToEveryType(CapturedOutput output) throws Exception {
        long mockExam = id(created("Regular 1", "2026-03-14", "[1, 2, 3]"));

        as("COORDINADOR", put(MOCK_EXAMS + "/" + mockExam).content(updateBody("Regular 2", "2026-03-21")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Regular 2"))
                .andExpect(jsonPath("$.examDate").value("2026-03-21"))
                .andExpect(jsonPath("$.exams.length()").value(3));
        assertThat(output.getOut()).contains("Mock exam updated: mockExamId=" + mockExam);
        assertThat(JsonPath.<List<String>>read(list("ADMIN"), "$[*].name")).containsExactly("Regular 2");
    }

    @Test
    void aNameIsUniquePerStage() throws Exception {
        created("Regular 1", "2026-03-14", "[1]");
        String duplicate = "Ya existe un examen con ese nombre en esta etapa.";

        create(createBody(regular, "REGULAR 1", "2026-03-21", "[2]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(duplicate));
        long other = id(created("Regular 2", "2026-03-21", "[1]"));
        as("ADMIN", put(MOCK_EXAMS + "/" + other).content(updateBody("regular 1", "2026-03-21")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(duplicate));

        // the same name in another cycle's stage is fine: names repeat every cycle
        long nextYear = cycle("2027-I", "2027-01-01", "2027-12-31", "PLANNED");
        long nextRegular = stage(nextYear, "Regular", "2027-03-01", "2027-06-30");
        create(createBody(nextRegular, "Regular 1", "2027-03-13", "[1]")).andExpect(status().isCreated());
    }

    @Test
    void theDateFallsWithinTheStage() throws Exception {
        String outside = "La fecha del examen debe estar dentro de las fechas de su etapa.";
        create(createBody(regular, "Regular 1", "2026-02-28", "[1]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(outside));
        create(createBody(regular, "Regular 1", "2026-07-01", "[1]")).andExpect(status().isConflict());
        long mockExam = id(created("Regular 1", "2026-06-30", "[1]"));
        as("ADMIN", put(MOCK_EXAMS + "/" + mockExam).content(updateBody("Regular 1", "2026-07-01")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(outside));

        // a stage without end date is open forward
        jdbc.update("UPDATE stage SET end_date = NULL WHERE id = ?", regular);
        create(createBody(regular, "Regular 2", "2026-12-01", "[1]")).andExpect(status().isCreated());
    }

    @Test
    void aMissingTypeIsAddedAndAnUnusedOneRemoved(CapturedOutput output) throws Exception {
        String json = created("Regular 1", "2026-03-14", "[1, 2]");
        long mockExam = id(json);
        String exams = MOCK_EXAMS + "/" + mockExam + "/exams";

        as("ADMIN", post(exams).content("{\"examTypeId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exams[*].examTypeName").value(contains("Ciencias", "Letras", "Arquitectura")));
        as("ADMIN", post(exams).content("{\"examTypeId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este examen ya tiene el tipo Ciencias."));

        long letras = examOf(json, "Letras");
        as("COORDINADOR", delete(exams + "/" + letras))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exams[*].examTypeName").value(contains("Ciencias", "Arquitectura")));
        assertThat(output.getOut()).contains("Exam type removed: mockExamId=" + mockExam + " examId=" + letras);
        // a removed type can come back
        as("ADMIN", post(exams).content("{\"examTypeId\":2}")).andExpect(status().isCreated());
    }

    @Test
    void aTypeWithResultsOrTheLastOneCannotBeRemoved() throws Exception {
        String json = created("Regular 1", "2026-03-14", "[1, 2]");
        long mockExam = id(json);
        String exams = MOCK_EXAMS + "/" + mockExam + "/exams/";
        giveResults(examOf(json, "Ciencias"));

        as("ADMIN", get(MOCK_EXAMS + "/" + mockExam))
                .andExpect(jsonPath("$.hasResults").value(true))
                .andExpect(jsonPath("$.exams[0].hasResults").value(true))
                .andExpect(jsonPath("$.exams[1].hasResults").value(false));
        as("ADMIN", delete(exams + examOf(json, "Ciencias")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El tipo Ciencias ya tiene resultados, así que no se puede quitar."));

        as("ADMIN", delete(exams + examOf(json, "Letras"))).andExpect(status().isOk());
        jdbc.update("DELETE FROM exam_result");
        as("ADMIN", delete(exams + examOf(json, "Ciencias")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Un examen necesita al menos un tipo. Si sobra, elimina el examen."));
    }

    @Test
    void aMockExamWithoutResultsIsDeletedFreesItsNameAndNoLongerTiesItsStage(CapturedOutput output) throws Exception {
        long mockExam = id(created("Regular 1", "2026-03-14", "[1, 2]"));
        String stage = "/api/v1/cycles/" + active + "/stages/" + regular;
        as("ADMIN", delete(stage)).andExpect(status().isConflict());

        as("COORDINADOR", delete(MOCK_EXAMS + "/" + mockExam)).andExpect(status().isNoContent());
        assertThat(output.getOut()).contains("Mock exam deleted: mockExamId=" + mockExam);
        assertThat(JsonPath.<List<Object>>read(list("ADMIN"), "$")).isEmpty();
        as("ADMIN", get(MOCK_EXAMS + "/" + mockExam)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM exam WHERE NOT deleted", Long.class)).isZero();

        created("Regular 1", "2026-03-14", "[1]");
        jdbc.update("UPDATE mock_exam SET deleted = true");
        as("ADMIN", delete(stage)).andExpect(status().isNoContent());
    }

    @Test
    void aMockExamWithResultsCannotBeDeleted() throws Exception {
        String json = created("Regular 1", "2026-03-14", "[1, 2]");
        giveResults(examOf(json, "Letras"));

        as("ADMIN", delete(MOCK_EXAMS + "/" + id(json)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este examen ya tiene resultados, así que no se puede eliminar."));
    }

    @Test
    void mockExamsOfAClosedCycleAreReadOnlyButAnEmptyOneCanBeDeleted() throws Exception {
        String closed = "Este ciclo está cerrado: sus exámenes ya no se pueden crear ni editar.";
        create(createBody(closedRegular, "Regular 1", "2025-09-06", "[1]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(closed));

        long mockExam = jdbc.queryForObject("INSERT INTO mock_exam (stage_id, name, exam_date) "
                + "VALUES (?, 'Regular 1', '2025-09-06') RETURNING id", Long.class, closedRegular);
        long exam = jdbc.queryForObject("INSERT INTO exam (mock_exam_id, exam_type_id) VALUES (?, 1) RETURNING id",
                Long.class, mockExam);
        jdbc.update("INSERT INTO exam (mock_exam_id, exam_type_id) VALUES (?, 2)", mockExam);
        String path = MOCK_EXAMS + "/" + mockExam;
        as("ADMIN", put(path).content(updateBody("Regular 2", "2025-09-06")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(closed));
        as("ADMIN", post(path + "/exams").content("{\"examTypeId\":3}")).andExpect(status().isConflict());
        as("ADMIN", delete(path + "/exams/" + exam)).andExpect(status().isConflict());

        as("ADMIN", delete(path)).andExpect(status().isNoContent());
    }

    @Test
    void aTutorReadsMockExamsButDoesNotWriteThem() throws Exception {
        String json = created("Regular 1", "2026-03-14", "[1, 2]");
        String path = MOCK_EXAMS + "/" + id(json);

        assertThat(JsonPath.<List<Object>>read(list("TUTOR"), "$")).hasSize(1);
        as("TUTOR", get(path)).andExpect(status().isOk());
        as("TUTOR", post(MOCK_EXAMS).content(createBody(regular, "Regular 2", "2026-03-21", "[1]")))
                .andExpect(status().isForbidden());
        as("TUTOR", put(path).content(updateBody("Regular 2", "2026-03-21"))).andExpect(status().isForbidden());
        as("TUTOR", post(path + "/exams").content("{\"examTypeId\":3}")).andExpect(status().isForbidden());
        as("TUTOR", delete(path + "/exams/" + examOf(json, "Letras"))).andExpect(status().isForbidden());
        as("TUTOR", delete(path)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "COORDINADOR"})
    void adminAndCoordinatorCreateMockExams(String role) throws Exception {
        as(role, post(MOCK_EXAMS).content(createBody(regular, "Regular 1", "2026-03-14", "[1]")))
                .andExpect(status().isCreated());
    }

    @Test
    void missingOrUnknownDataIsRejectedInSpanish() throws Exception {
        create(createBody(regular, " ", "2026-03-14", "[1]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El examen necesita un nombre."));
        create(createBody(regular, "Regular 1", null, "[1]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Ingresa la fecha del examen."));
        create(createBody(regular, "Regular 1", "2026-03-14", "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Elige al menos un tipo de examen."));
        create(createBody(null, "Regular 1", "2026-03-14", "[1]")).andExpect(status().isBadRequest());
        as("ADMIN", get(MOCK_EXAMS)).andExpect(status().isBadRequest());

        create(createBody(999999L, "Regular 1", "2026-03-14", "[1]"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos la etapa solicitada."));
        create(createBody(regular, "Regular 1", "2026-03-14", "[1, 999999]"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el tipo de examen solicitado."));
        as("ADMIN", get(MOCK_EXAMS + "/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el examen solicitado."));
        as("ADMIN", get(MOCK_EXAMS).param("stageId", "999999")).andExpect(status().isNotFound());
        long mockExam = id(created("Regular 1", "2026-03-14", "[1]"));
        as("ADMIN", delete(MOCK_EXAMS + "/" + mockExam + "/exams/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Este examen no tiene ese tipo de examen."));
        assertThat(JsonPath.<List<Object>>read(list("ADMIN"), "$")).hasSize(1);
    }

    @Test
    void readingRequiresASession() throws Exception {
        mockMvc.perform(get(MOCK_EXAMS).param("stageId", "1")).andExpect(status().isUnauthorized());
    }
}
