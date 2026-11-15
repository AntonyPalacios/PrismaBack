package pe.com.edu.prisma.exams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Exams of a stage (REQ-EXAMS-01). Cycles and stages are inserted straight
 * into the database: an active 2026-I with its "Regular" stage (March to
 * June) and a closed 2025-II with a stage of the same name. Exam types come
 * from the seed (1 Ciencias, 2 Letras, 3 Arquitectura).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class ExamApiIT {

    private static final String EXAMS = "/api/v1/exams";

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

    /** One result for the exam (the import is a later slice): it locks the type and blocks deleting. */
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

    private static String updateBody(String name, String date, Long typeId) {
        return "{\"name\":\"" + name + "\",\"examDate\":\"" + date + "\",\"examTypeId\":" + typeId + "}";
    }

    private ResultActions as(String role, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user("someone").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions create(String body) throws Exception {
        return as("ADMIN", post(EXAMS).content(body));
    }

    private List<Integer> createdIds(String name, String date, String typeIds) throws Exception {
        String json = create(createBody(regular, name, date, typeIds)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[*].id");
    }

    private String list(String role) throws Exception {
        return as(role, get(EXAMS).param("stageId", String.valueOf(regular))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void aMockExamCreatesOneExamPerTypeInOneGo(CapturedOutput output) throws Exception {
        create(createBody(regular, " Regular 1 ", "2026-03-14", "[1, 2, 3]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("Regular 1"))))
                .andExpect(jsonPath("$[*].examTypeName").value(org.hamcrest.Matchers.contains("Ciencias", "Letras", "Arquitectura")))
                .andExpect(jsonPath("$[0].stageId").value(regular))
                .andExpect(jsonPath("$[0].examDate").value("2026-03-14"))
                .andExpect(jsonPath("$[0].hasResults").value(false));

        assertThat(output.getOut()).contains("Exams created: stageId=" + regular);
    }

    @Test
    void examsAreListedNewestFirstWithTheExamsOfAMockExamTogether() throws Exception {
        createdIds("Medición", "2026-03-02", "[1, 2]");
        createdIds("Regular 1", "2026-03-14", "[2, 1]");

        String json = list("TUTOR");
        assertThat(JsonPath.<List<String>>read(json, "$[*].name"))
                .containsExactly("Regular 1", "Regular 1", "Medición", "Medición");
        assertThat(JsonPath.<List<String>>read(json, "$[*].examTypeName"))
                .containsExactly("Ciencias", "Letras", "Ciencias", "Letras");
    }

    @Test
    void theSameTypeSentTwiceIsOneExam() throws Exception {
        assertThat(createdIds("Regular 1", "2026-03-14", "[1, 1]")).hasSize(1);
    }

    @Test
    void aNameIsUniquePerStageAndTypeOnly() throws Exception {
        createdIds("Regular 1", "2026-03-14", "[1]");

        create(createBody(regular, "REGULAR 1", "2026-03-21", "[2, 1]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un examen de Ciencias con ese nombre en esta etapa."));
        // all or nothing: Letras was not created either
        assertThat(JsonPath.<List<Object>>read(list("ADMIN"), "$")).hasSize(1);

        // another type, or the same name in another cycle's stage, is fine
        createdIds("Regular 1", "2026-03-14", "[2]");
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
        create(createBody(regular, "Regular 1", "2026-06-30", "[1]")).andExpect(status().isCreated());

        // a stage without end date is open forward
        jdbc.update("UPDATE stage SET end_date = NULL WHERE id = ?", regular);
        create(createBody(regular, "Regular 2", "2026-12-01", "[1]")).andExpect(status().isCreated());
    }

    @Test
    void examsOfAClosedCycleCannotBeCreatedOrEdited() throws Exception {
        String closed = "Este ciclo está cerrado: sus exámenes ya no se pueden crear ni editar.";
        create(createBody(closedRegular, "Regular 1", "2025-09-06", "[1]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(closed));

        long exam = jdbc.queryForObject("INSERT INTO exam (stage_id, exam_type_id, name, exam_date) "
                + "VALUES (?, 1, 'Regular 1', '2025-09-06') RETURNING id", Long.class, closedRegular);
        as("ADMIN", put(EXAMS + "/" + exam).content(updateBody("Regular 2", "2025-09-06", 1L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(closed));
    }

    @Test
    void anExamIsEditedAndItsTypeLocksOnceItHasResults(CapturedOutput output) throws Exception {
        long exam = createdIds("Regular 1", "2026-03-14", "[1]").get(0);

        as("COORDINADOR", put(EXAMS + "/" + exam).content(updateBody("Regular 2", "2026-03-21", 2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Regular 2"))
                .andExpect(jsonPath("$.examDate").value("2026-03-21"))
                .andExpect(jsonPath("$.examTypeName").value("Letras"));
        assertThat(output.getOut()).contains("Exam updated: examId=" + exam + " examTypeId=2");

        giveResults(exam);
        as("ADMIN", get(EXAMS + "/" + exam)).andExpect(jsonPath("$.hasResults").value(true));
        as("ADMIN", put(EXAMS + "/" + exam).content(updateBody("Regular 2", "2026-03-21", 1L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este examen ya tiene resultados: su tipo de examen ya no se puede cambiar."));
        // name and date stay editable
        as("ADMIN", put(EXAMS + "/" + exam).content(updateBody("Regular 3", "2026-03-28", 2L)))
                .andExpect(status().isOk());
    }

    @Test
    void anExamWithoutResultsIsDeletedAndFreesItsName(CapturedOutput output) throws Exception {
        long exam = createdIds("Regular 1", "2026-03-14", "[1]").get(0);

        as("COORDINADOR", delete(EXAMS + "/" + exam)).andExpect(status().isNoContent());
        assertThat(output.getOut()).contains("Exam deleted: examId=" + exam);
        assertThat(JsonPath.<List<Object>>read(list("ADMIN"), "$")).isEmpty();
        as("ADMIN", get(EXAMS + "/" + exam)).andExpect(status().isNotFound());
        createdIds("Regular 1", "2026-03-14", "[1]");
    }

    @Test
    void aDeletedExamNoLongerTiesItsStage() throws Exception {
        long exam = createdIds("Regular 1", "2026-03-14", "[1]").get(0);
        String stage = "/api/v1/cycles/" + active + "/stages/" + regular;
        as("ADMIN", delete(stage)).andExpect(status().isConflict());

        as("ADMIN", delete(EXAMS + "/" + exam)).andExpect(status().isNoContent());
        as("ADMIN", delete(stage)).andExpect(status().isNoContent());
    }

    @Test
    void anExamWithResultsCannotBeDeleted() throws Exception {
        long exam = createdIds("Regular 1", "2026-03-14", "[1]").get(0);
        giveResults(exam);

        as("ADMIN", delete(EXAMS + "/" + exam))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este examen ya tiene resultados, así que no se puede eliminar."));
    }

    @Test
    void aTutorReadsExamsButDoesNotWriteThem() throws Exception {
        long exam = createdIds("Regular 1", "2026-03-14", "[1]").get(0);

        assertThat(JsonPath.<List<Object>>read(list("TUTOR"), "$")).hasSize(1);
        as("TUTOR", get(EXAMS + "/" + exam)).andExpect(status().isOk());
        as("TUTOR", post(EXAMS).content(createBody(regular, "Regular 2", "2026-03-21", "[1]")))
                .andExpect(status().isForbidden());
        as("TUTOR", put(EXAMS + "/" + exam).content(updateBody("Regular 2", "2026-03-21", 1L)))
                .andExpect(status().isForbidden());
        as("TUTOR", delete(EXAMS + "/" + exam)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "COORDINADOR"})
    void adminAndCoordinatorCreateExams(String role) throws Exception {
        as(role, post(EXAMS).content(createBody(regular, "Regular 1", "2026-03-14", "[1]")))
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
        as("ADMIN", get(EXAMS)).andExpect(status().isBadRequest());

        create(createBody(999999L, "Regular 1", "2026-03-14", "[1]"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos la etapa solicitada."));
        create(createBody(regular, "Regular 1", "2026-03-14", "[1, 999999]"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el tipo de examen solicitado."));
        as("ADMIN", get(EXAMS + "/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el examen solicitado."));
        as("ADMIN", get(EXAMS).param("stageId", "999999")).andExpect(status().isNotFound());
        assertThat(JsonPath.<List<Object>>read(list("ADMIN"), "$")).isEmpty();
    }

    @Test
    void readingRequiresASession() throws Exception {
        mockMvc.perform(get(EXAMS).param("stageId", "1")).andExpect(status().isUnauthorized());
    }
}
