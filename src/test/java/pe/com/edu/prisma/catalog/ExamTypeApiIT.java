package pe.com.edu.prisma.catalog;

import static org.hamcrest.Matchers.contains;
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
import java.util.UUID;
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
import org.springframework.test.web.servlet.ResultActions;
import pe.com.edu.prisma.config.ContainersConfig;

/**
 * Exam types: an admin creates and edits name + courses in one request (all
 * or nothing, courses fully replaced); names are unique ignoring case; they
 * are never deactivated. Seed courses: 1 Lectura, 2 Números y Operaciones,
 * 3 Álgebra, 4 Geometría, 5 Trigonometría, 6 Estadística.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class ExamTypeApiIT {

    @Autowired
    private MockMvc mockMvc;

    private static String uniqueName() {
        return "Tipo " + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String body(String name, String courseIds) {
        return "{\"name\":\"" + name + "\",\"courseIds\":[" + courseIds + "]}";
    }

    private ResultActions create(String name, String courseIds) throws Exception {
        return mockMvc.perform(post("/api/v1/exam-types").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body(name, courseIds)));
    }

    private long createdId(String name, String courseIds) throws Exception {
        String json = create(name, courseIds).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private ResultActions update(long id, String name, String courseIds) throws Exception {
        return mockMvc.perform(put("/api/v1/exam-types/" + id).with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body(name, courseIds)));
    }

    @Test
    @WithMockUser(roles = "TUTOR")
    void seededTypesAreListedAlphabeticallyWithTheirCoursesAlphabetically() throws Exception {
        mockMvc.perform(get("/api/v1/exam-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Arquitectura')].courseNames[*]",
                        contains("Álgebra", "Geometría", "Lectura", "Números y Operaciones")))
                .andExpect(jsonPath("$[?(@.name == 'Ciencias')].courseNames[0]").value(contains("Álgebra")));
    }

    @Test
    void adminCreatesAnExamTypeWithItsCoursesInOneRequest() throws Exception {
        String name = uniqueName();
        create(name, "6,1,3")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/exam-types/")))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.courseNames", contains("Álgebra", "Estadística", "Lectura")))
                .andExpect(jsonPath("$.courseIds", contains(3, 6, 1)));
    }

    @Test
    void editingReplacesNameAndTheWholeCourseList() throws Exception {
        long id = createdId(uniqueName(), "1,2,3");
        String renamed = uniqueName();

        update(id, renamed, "4")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed))
                .andExpect(jsonPath("$.courseNames", contains("Geometría")));

        mockMvc.perform(get("/api/v1/exam-types/" + id).with(user("t").roles("TUTOR")))
                .andExpect(jsonPath("$.courseIds", contains(4)));
    }

    @Test
    void anUnknownCourseRejectsTheWholeRequestAndSavesNothing() throws Exception {
        long id = createdId(uniqueName(), "1");
        String attemptedName = uniqueName();

        update(id, attemptedName, "1,999")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Uno o más cursos seleccionados no existen."));

        mockMvc.perform(get("/api/v1/exam-types/" + id).with(user("t").roles("TUTOR")))
                .andExpect(jsonPath("$.name").value(org.hamcrest.Matchers.not(attemptedName)))
                .andExpect(jsonPath("$.courseIds", contains(1)));

        create(uniqueName(), "999").andExpect(status().isConflict());
    }

    @Test
    void namesAreUniqueIgnoringCase() throws Exception {
        create("ciencias", "1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un tipo de examen con ese nombre."));

        long id = createdId(uniqueName(), "1");
        update(id, "LETRAS", "1").andExpect(status().isConflict());
    }

    @Test
    void aTypeCanKeepItsOwnNameWhenEdited() throws Exception {
        String name = uniqueName();
        long id = createdId(name, "1");

        update(id, name.toUpperCase(), "1,2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name.toUpperCase()));
    }

    @Test
    void invalidRequestsAnswer400InSpanish() throws Exception {
        create("", "1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("necesita un nombre")));
        create("x".repeat(51), "1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("demasiado largo")));
        create(uniqueName(), "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("al menos un curso")));
    }

    @Test
    void editingAnUnknownTypeAnswers404() throws Exception {
        update(999_999, uniqueName(), "1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el tipo de examen solicitado."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TUTOR", "COORDINADOR"})
    void onlyAdminsWriteButEveryRoleReads(String role) throws Exception {
        mockMvc.perform(get("/api/v1/exam-types").with(user("u").roles(role))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/exam-types").with(user("u").roles(role)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(uniqueName(), "1")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/exam-types/1").with(user("u").roles(role)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(uniqueName(), "1")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void examTypesCannotBeDeactivatedAndCoursesHaveNoSeparateEndpoint() throws Exception {
        mockMvc.perform(delete("/api/v1/exam-types/1").with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/v1/exam-types/1/courses").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courseIds\":[1]}"))
                .andExpect(status().isNotFound());
    }
}
