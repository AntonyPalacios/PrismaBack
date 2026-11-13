package pe.com.edu.prisma.students;

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
 * Students (REQ-STUDENTS-01, -04): list, search, register, edit, retire and
 * reactivate. No student
 * is seeded, so every test starts from an empty table. The people here are
 * invented.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class StudentApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void emptyTables() {
        jdbc.update("DELETE FROM student_stage");
        jdbc.update("DELETE FROM student");
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static String body(String firstName, String lastNamePat, String lastNameMat, String dni) {
        return body(firstName, lastNamePat, lastNameMat, dni, null, null, null);
    }

    private static String body(String firstName, String lastNamePat, String lastNameMat, String dni,
            String email, String phone, String birthDate) {
        return "{\"firstName\":" + quote(firstName) + ",\"lastNamePat\":" + quote(lastNamePat)
                + ",\"lastNameMat\":" + quote(lastNameMat) + ",\"dni\":" + quote(dni)
                + ",\"email\":" + quote(email) + ",\"phone\":" + quote(phone)
                + ",\"birthDate\":" + quote(birthDate) + "}";
    }

    private ResultActions as(String role, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user("someone").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return as("ADMIN", request);
    }

    private ResultActions create(String content) throws Exception {
        return asAdmin(post("/api/v1/students").content(content));
    }

    private long createdId(String firstName, String lastNamePat, String lastNameMat, String dni) throws Exception {
        String json = create(body(firstName, lastNamePat, lastNameMat, dni)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private List<String> listedSurnames(String query) throws Exception {
        String json = asAdmin(get("/api/v1/students" + query)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[*].lastNamePat");
    }

    @Test
    void adminRegistersAStudentWithEveryField() throws Exception {
        create(body("  Lucía Fernanda ", " Quispe ", "Mamani", "72345678", "lucia@correo.pe", "987654321", "2009-05-17"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/students/")))
                .andExpect(jsonPath("$.firstName").value("Lucía Fernanda"))
                .andExpect(jsonPath("$.lastNamePat").value("Quispe"))
                .andExpect(jsonPath("$.lastNameMat").value("Mamani"))
                .andExpect(jsonPath("$.dni").value("72345678"))
                .andExpect(jsonPath("$.email").value("lucia@correo.pe"))
                .andExpect(jsonPath("$.phone").value("987654321"))
                .andExpect(jsonPath("$.birthDate").value("2009-05-17"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void onlyNamesAndPaternalSurnameAreRequiredAndEmptyTextIsStoredAsNothing() throws Exception {
        String json = create(body("Mateo", "Rojas", "", "  ", "", " ", null))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();

        asAdmin(get("/api/v1/students/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastNameMat").doesNotExist())
                .andExpect(jsonPath("$.dni").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.birthDate").doesNotExist());
    }

    @Test
    void studentsAreListedBySurnamesIgnoringCaseAndAccents() throws Exception {
        createdId("Zoe", "Vargas", null, null);
        createdId("Bruno", "álvarez", "Soto", null);
        createdId("Ana", "Alvarez", "Ruiz", null);
        createdId("Carla", "Alvarez", null, null); // no maternal surname: after the ones that have it
        createdId("Abel", "Alvarez", "Ruiz", null);

        String json = asAdmin(get("/api/v1/students")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> firstNames = JsonPath.read(json, "$[*].firstName");

        assertThat(firstNames).containsExactly("Abel", "Ana", "Bruno", "Carla", "Zoe");
    }

    @Test
    void searchFindsByNameOrDniIgnoringCaseAndAccents() throws Exception {
        createdId("Lucía", "Quispe", "Mamani", "72345678");
        createdId("Mateo", "Rojas", "Peña", "CE00123456");
        createdId("Valeria", "Núñez", null, null);

        assertThat(listedSurnames("?search=lucia")).containsExactly("Quispe");
        assertThat(listedSurnames("?search=NUÑEZ")).containsExactly("Núñez");
        assertThat(listedSurnames("?search=nunez")).containsExactly("Núñez");
        // several words, in any order, each matching part of a name
        assertThat(listedSurnames("?search=mam luc")).containsExactly("Quispe");
        assertThat(listedSurnames("?search=7234")).containsExactly("Quispe");
        assertThat(listedSurnames("?search=ce001")).containsExactly("Rojas");
        assertThat(listedSurnames("?search=nadie")).isEmpty();
        // an empty search is no filter
        assertThat(listedSurnames("?search=")).hasSize(3);
        asAdmin(get("/api/v1/students").param("search", "   ")).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void theDniIsUniqueWhenPresentButSeveralStudentsCanHaveNone() throws Exception {
        long first = createdId("Lucía", "Quispe", null, "72345678");
        long second = createdId("Mateo", "Rojas", null, null);
        createdId("Valeria", "Núñez", null, null);

        create(body("Otra", "Persona", null, " 72345678 "))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un alumno con ese DNI."));
        asAdmin(put("/api/v1/students/" + second).content(body("Mateo", "Rojas", null, "72345678")))
                .andExpect(status().isConflict());

        // a student keeps their own DNI when edited
        asAdmin(put("/api/v1/students/" + first).content(body("Lucía", "Quispe", "Mamani", "72345678")))
                .andExpect(status().isOk());
    }

    @Test
    void theDniAcceptsEightToTwelveLettersOrDigitsAndIsStoredInUpperCase() throws Exception {
        create(body("Mateo", "Rojas", null, "ce00123456"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").value("CE00123456"));
        // the same card typed in another case is the same DNI
        create(body("Otro", "Alumno", null, "CE00123456")).andExpect(status().isConflict());

        String invalid = "El DNI debe tener entre 8 y 12 letras o números.";
        create(body("A", "B", null, "1234567")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(invalid));
        create(body("A", "B", null, "1234567890123")).andExpect(status().isBadRequest());
        create(body("A", "B", null, "7234-5678")).andExpect(status().isBadRequest());
    }

    @Test
    void adminEditsAStudent() throws Exception {
        long id = createdId("Lucia", "Quispe", null, null);

        asAdmin(put("/api/v1/students/" + id)
                .content(body("Lucía", "Quispe", "Mamani", "72345678", "lucia@correo.pe", null, "2009-05-17")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Lucía"))
                .andExpect(jsonPath("$.lastNameMat").value("Mamani"))
                .andExpect(jsonPath("$.dni").value("72345678"))
                .andExpect(jsonPath("$.birthDate").value("2009-05-17"));

        // optional data can be cleared again
        asAdmin(put("/api/v1/students/" + id).content(body("Lucía", "Quispe", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void invalidRequestsAnswer400InSpanish() throws Exception {
        create(body("", "Quispe", null, null)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("nombres del alumno")));
        create(body("Lucía", " ", null, null)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("apellido paterno del alumno")));
        create(body("x".repeat(151), "Quispe", null, null)).andExpect(status().isBadRequest());
        create(body("Lucía", "Quispe", null, null, "no-es-correo", null, null)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El correo no tiene un formato válido."));
        create(body("Lucía", "Quispe", null, null, null, "9".repeat(31), null)).andExpect(status().isBadRequest());
        create(body("Lucía", "Quispe", null, null, null, null, "2999-01-01")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La fecha de nacimiento no puede ser futura."));

        asAdmin(get("/api/v1/students")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknownStudentAnswers404InSpanish() throws Exception {
        asAdmin(get("/api/v1/students/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos el alumno solicitado."));
        asAdmin(put("/api/v1/students/999999").content(body("Lucía", "Quispe", null, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminRetiresAndReactivatesAStudentWithoutTouchingTheirData(CapturedOutput output) throws Exception {
        long id = createdId("Lucía", "Quispe", "Mamani", "72345678");

        asAdmin(delete("/api/v1/students/" + id)).andExpect(status().isNoContent());
        // still there, only inactive: the row is never physically deleted
        asAdmin(get("/api/v1/students/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.dni").value("72345678"));

        asAdmin(post("/api/v1/students/" + id + "/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.firstName").value("Lucía"))
                .andExpect(jsonPath("$.lastNameMat").value("Mamani"))
                .andExpect(jsonPath("$.dni").value("72345678"));

        assertThat(output.getOut())
                .contains("Student deactivated: studentId=" + id)
                .contains("Student reactivated: studentId=" + id);
    }

    @Test
    void retiringAnInactiveOrReactivatingAnActiveStudentAnswers409() throws Exception {
        long id = createdId("Lucía", "Quispe", null, null);

        asAdmin(post("/api/v1/students/" + id + "/reactivate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este alumno ya está activo."));
        asAdmin(delete("/api/v1/students/" + id)).andExpect(status().isNoContent());
        asAdmin(delete("/api/v1/students/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este alumno ya está dado de baja."));
    }

    @Test
    void anInactiveStudentCannotBeEditedUntilReactivated() throws Exception {
        long id = createdId("Lucía", "Quispe", null, null);
        asAdmin(delete("/api/v1/students/" + id)).andExpect(status().isNoContent());

        asAdmin(put("/api/v1/students/" + id).content(body("Lucía", "Quispe", "Mamani", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este alumno está dado de baja: reactívalo para editar sus datos."));

        asAdmin(post("/api/v1/students/" + id + "/reactivate")).andExpect(status().isOk());
        asAdmin(put("/api/v1/students/" + id).content(body("Lucía", "Quispe", "Mamani", null)))
                .andExpect(status().isOk());
    }

    @Test
    void inactiveStudentsAreListedOnlyOnRequestAndStillSearchable() throws Exception {
        createdId("Mateo", "Rojas", null, null);
        long retired = createdId("Lucía", "Quispe", null, "72345678");
        asAdmin(delete("/api/v1/students/" + retired)).andExpect(status().isNoContent());

        assertThat(listedSurnames("")).containsExactly("Rojas");
        assertThat(listedSurnames("?includeInactive=false")).containsExactly("Rojas");
        assertThat(listedSurnames("?includeInactive=true")).containsExactly("Quispe", "Rojas");
        assertThat(listedSurnames("?search=7234")).isEmpty();
        assertThat(listedSurnames("?search=7234&includeInactive=true")).containsExactly("Quispe");
    }

    @ParameterizedTest
    @ValueSource(strings = {"TUTOR", "COORDINADOR"})
    void everyRoleSeesInactiveStudentsButOnlyAdminRetiresOrReactivates(String role) throws Exception {
        long id = createdId("Lucía", "Quispe", null, null);

        as(role, delete("/api/v1/students/" + id)).andExpect(status().isForbidden());
        asAdmin(delete("/api/v1/students/" + id)).andExpect(status().isNoContent());
        as(role, post("/api/v1/students/" + id + "/reactivate")).andExpect(status().isForbidden());

        as(role, get("/api/v1/students?includeInactive=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].active").value(false));
        as(role, get("/api/v1/students/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void retiringOrReactivatingAnUnknownStudentAnswers404() throws Exception {
        asAdmin(delete("/api/v1/students/999999")).andExpect(status().isNotFound());
        asAdmin(post("/api/v1/students/999999/reactivate")).andExpect(status().isNotFound());
    }

    @Test
    void personalDataNeverReachesTheLogsInClearText(CapturedOutput output) throws Exception {
        String everything = body("Zoraida Itzel", "Huamanchumo", "Ccoyllo", "71234987",
                "zoraida.itzel@correo.pe", "912345678", "2009-05-17");
        String json = create(everything).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();
        // a rejected request is logged with its (sanitized) arguments
        create(everything).andExpect(status().isConflict());
        asAdmin(put("/api/v1/students/" + id).content(everything)).andExpect(status().isOk());
        asAdmin(get("/api/v1/students?search=Huamanchumo")).andExpect(status().isOk());

        String log = output.getOut();
        // each write leaves its INFO line, with nothing but the id
        assertThat(log).contains("Student created: studentId=" + id).contains("Student updated: studentId=" + id);
        // the rejected request shows the data only masked
        assertThat(log).contains("409 on POST /api/v1/students")
                .contains("firstName=Zor*** Itz***", "lastNamePat=Hua***", "lastNameMat=Cco***",
                        "dni=*****987", "email=z***@correo.pe", "phone=***", "birthDate=***", "search=***");
        assertThat(log).doesNotContain("Zoraida", "Itzel", "Huamanchumo", "Ccoyllo", "71234987",
                "zoraida.itzel", "912345678", "2009-05-17");
    }

    @Test
    void aTutorAlsoRegistersAndEditsStudents() throws Exception {
        String json = as("TUTOR", post("/api/v1/students").content(body("Lucía", "Quispe", null, null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();

        as("TUTOR", put("/api/v1/students/" + id).content(body("Lucía", "Quispe", "Mamani", null)))
                .andExpect(status().isOk());
        as("TUTOR", get("/api/v1/students")).andExpect(status().isOk());
    }

    @Test
    void aCoordinatorOnlyReadsStudents() throws Exception {
        long id = createdId("Lucía", "Quispe", null, null);

        as("COORDINADOR", get("/api/v1/students")).andExpect(status().isOk());
        as("COORDINADOR", get("/api/v1/students?search=quispe")).andExpect(status().isOk());
        as("COORDINADOR", get("/api/v1/students/" + id)).andExpect(status().isOk());
        as("COORDINADOR", post("/api/v1/students").content(body("Otro", "Alumno", null, null)))
                .andExpect(status().isForbidden());
        as("COORDINADOR", put("/api/v1/students/" + id).content(body("Lucía", "Quispe", null, null)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/students", "/api/v1/students/1"})
    void readingRequiresASession(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
}
