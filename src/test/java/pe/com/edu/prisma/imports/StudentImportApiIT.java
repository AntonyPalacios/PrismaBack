package pe.com.edu.prisma.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import pe.com.edu.prisma.config.ContainersConfig;
import pe.com.edu.prisma.security.AuthenticatedUser;

/**
 * Importing students from the template (REQ-IMPORTS-02) through a review
 * batch (REQ-IMPORTS-01). The spreadsheets are built here; every person is
 * invented. The seed brings the careers and the tutor Silvia Valle Gómez;
 * an active cycle with its "Regular" stage is inserted for enrolling.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class StudentImportApiIT {

    private static final String IMPORTS = "/api/v1/student-imports";
    private static final String BATCHES = "/api/v1/import-batches";
    private static final String[] HEADERS = {"Nombres *", "Apellido paterno *", "Apellido materno", "DNI", "Correo",
            "Teléfono", "Fecha de nacimiento", "Carrera", "Tutor"};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long me;
    private long stage;
    private long lucia;
    private long mateo;
    private long silvia;

    @BeforeEach
    void seed() {
        emptyTables();
        me = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'apalaciosc0109@gmail.com'", Long.class);
        silvia = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'silvia.valleg@prisma.edu.pe'", Long.class);
        long cycle = jdbc.queryForObject("INSERT INTO cycle (name, start_date, end_date, status) "
                + "VALUES ('2026-I', '2026-01-01', '2026-12-31', 'ACTIVE') RETURNING id", Long.class);
        stage = jdbc.queryForObject("INSERT INTO stage (cycle_id, name, start_date, end_date) "
                + "VALUES (?, 'Regular', '2026-03-01', '2026-06-30') RETURNING id", Long.class, cycle);
        lucia = student("Lucía", "Quispe", "Mamani", "72345678", true);
        mateo = student("Mateo", "Rojas", null, null, true);
    }

    @AfterEach
    void emptyTables() {
        jdbc.update("DELETE FROM import_row");
        jdbc.update("DELETE FROM import_batch");
        jdbc.update("DELETE FROM tutor_assignment");
        jdbc.update("DELETE FROM student_stage");
        jdbc.update("DELETE FROM student");
        jdbc.update("DELETE FROM stage");
        jdbc.update("DELETE FROM cycle");
    }

    private long student(String firstName, String lastNamePat, String lastNameMat, String dni, boolean active) {
        return jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat, last_name_mat, dni, active) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, firstName, lastNamePat, lastNameMat, dni, active);
    }

    /** A spreadsheet with the template's headers and these rows (null = empty cell). */
    private static byte[] xlsx(String[]... rows) throws Exception {
        return xlsxWithHeaders(HEADERS, rows);
    }

    private static byte[] xlsxWithHeaders(String[] headers, String[]... rows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Alumnos");
            write(sheet.createRow(0), headers);
            for (int i = 0; i < rows.length; i++) {
                write(sheet.createRow(i + 1), rows[i]);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static void write(Row row, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                row.createCell(i).setCellValue(values[i]);
            }
        }
    }

    private static String[] row(String... values) {
        return Arrays.copyOf(values, 9);
    }

    /** A request as a user with those roles (the principal the JWT filter would set). */
    private ResultActions as(String role, AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
        AuthenticatedUser principal = () -> me;
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return mockMvc.perform(request.with(authentication(auth)).with(csrf()));
    }

    private ResultActions upload(String role, byte[] file, Long stageId) throws Exception {
        var request = multipart(IMPORTS).file(new MockMultipartFile("file", "alumnos.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file));
        if (stageId != null) {
            request.param("stageId", String.valueOf(stageId));
        }
        return as(role, request);
    }

    private String uploaded(byte[] file, Long stageId) throws Exception {
        return upload("ADMIN", file, stageId).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private static long rowId(String json, int rowNumber) {
        List<Integer> ids = JsonPath.read(json, "$.rows[?(@.rowNumber == " + rowNumber + ")].id");
        return ids.get(0);
    }

    private ResultActions resolve(String role, long batch, long row, String action, Long studentId) throws Exception {
        return as(role, put(BATCHES + "/" + batch + "/rows/" + row).contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"" + action + "\",\"studentId\":" + studentId + "}"));
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    @Test
    void theTemplateHasTheNineColumns() throws Exception {
        byte[] file = as("TUTOR", get(IMPORTS + "/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("plantilla-alumnos.xlsx")))
                .andReturn().getResponse().getContentAsByteArray();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            Row headers = workbook.getSheetAt(0).getRow(0);
            assertThat(headers.getLastCellNum()).isEqualTo((short) 9);
            assertThat(headers.getCell(0).getStringCellValue()).isEqualTo("Nombres *");
            assertThat(headers.getCell(8).getStringCellValue()).isEqualTo("Tutor");
        }
    }

    @Test
    void aDniOfZerosIsNoDni() throws Exception {
        // a placeholder while the DNI is unknown: never a repeated DNI, never stored
        long batch = id(uploaded(xlsx(row("Mateo", "Rojas", null, "0"), row("Dario", "Torres", null, "00000000")), null));
        as("ADMIN", post(BATCHES + "/" + batch + "/confirm")).andExpect(status().isOk());
        assertThat(count("SELECT count(*) FROM student WHERE last_name_pat IN ('Rojas', 'Torres') AND dni IS NULL")).isEqualTo(2);
    }

    @Test
    void anIdentificationHasAtLeastSevenDigits() throws Exception {
        // a short number is no DNI (it used to reject the whole file); seven digits get their leading zero back
        long batch = id(uploaded(xlsx(row("Mateo", "Rojas", null, "11"), row("Dario", "Torres", null, "6184164")), null));
        as("ADMIN", post(BATCHES + "/" + batch + "/confirm")).andExpect(status().isOk());
        assertThat(count("SELECT count(*) FROM student WHERE last_name_pat = 'Rojas' AND dni IS NULL")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM student WHERE last_name_pat = 'Torres' AND dni = '06184164'")).isEqualTo(1);
    }

    @Test
    void uploadingReadsEveryRowAndWritesNothingReal(CapturedOutput output) throws Exception {
        String json = uploaded(xlsx(
                row("Valeria", "Díaz", "Soto", "70011223", "valeria@correo.pe", "987654321", "17/05/2009", "letras", "SILVIA"),
                row("Lucia", "Quispe", null, " 72345678 "),
                row("Mateo", "Roja"),
                row("Diego", "Mendoza", null, null, null, null, null, "Medicina", "Pedro")), stage);

        long batch = id(json);
        assertThat(JsonPath.<String>read(json, "$.status")).isEqualTo("PENDING_REVIEW");
        assertThat(JsonPath.<String>read(json, "$.stageLabel")).isEqualTo("2026-I · Regular");
        assertThat(JsonPath.<Integer>read(json, "$.totalRows")).isEqualTo(4);
        assertThat(JsonPath.<Integer>read(json, "$.newRows")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(json, "$.matchedRows")).isEqualTo(1);
        assertThat(JsonPath.<Integer>read(json, "$.unresolvedRows")).isEqualTo(1);

        // a new student, whose career and tutor (by first name) were found
        assertThat(JsonPath.<String>read(json, "$.rows[0].status")).isEqualTo("NEW_STUDENT");
        assertThat(JsonPath.<String>read(json, "$.rows[0].fullName")).isEqualTo("Díaz Soto, Valeria");
        assertThat(JsonPath.<String>read(json, "$.rows[0].birthDate")).isEqualTo("2009-05-17");
        assertThat(JsonPath.<String>read(json, "$.rows[0].careerName")).isEqualTo("Letras");
        assertThat(JsonPath.<String>read(json, "$.rows[0].tutorName")).isEqualTo("Valle Gómez, Silvia");
        // the DNI identifies a registered student
        assertThat(JsonPath.<String>read(json, "$.rows[1].status")).isEqualTo("AUTO_MATCHED");
        assertThat(JsonPath.<Integer>read(json, "$.rows[1].student.id")).isEqualTo((int) lucia);
        // a very similar name goes to a person, with the candidate
        assertThat(JsonPath.<String>read(json, "$.rows[2].status")).isEqualTo("UNRESOLVED");
        assertThat(JsonPath.<List<Integer>>read(json, "$.rows[2].candidates[*].id")).containsExactly((int) mateo);
        // an unknown career or tutor never rejects: the row stays without them
        assertThat(JsonPath.<String>read(json, "$.rows[3].status")).isEqualTo("NEW_STUDENT");
        assertThat(JsonPath.<Object>read(json, "$.rows[3].careerName")).isNull();
        assertThat(JsonPath.<String>read(json, "$.rows[3].careerText")).isEqualTo("Medicina");
        assertThat(JsonPath.<Object>read(json, "$.rows[3].tutorName")).isNull();

        assertThat(count("SELECT count(*) FROM student")).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM student_stage")).isZero();
        assertThat(output.getOut()).contains("Student import uploaded: batchId=" + batch + " stageId=" + stage + " rows=4");
        // personal data never reaches the logs
        assertThat(output.getOut()).doesNotContain("Valeria", "70011223", "valeria@correo.pe");
    }

    @Test
    void confirmingRegistersEnrollsAndGivesTutors(CapturedOutput output) throws Exception {
        String json = uploaded(xlsx(
                row("Valeria", "Díaz", "Soto", "70011223", null, null, null, "Letras", "silvia"),
                row("Lucia", "Quispe", null, "72345678", null, null, null, "Ciencias"),
                row("Mateo", "Roja")), stage);
        long batch = id(json);

        as("ADMIN", post(BATCHES + "/" + batch + "/confirm"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Aún hay filas sin resolver: resuélvelas o ignóralas antes de confirmar."));
        resolve("ADMIN", batch, rowId(json, 4), "MATCH", mateo).andExpect(status().isOk())
                .andExpect(jsonPath("$.unresolvedRows").value(0));

        as("ADMIN", post(BATCHES + "/" + batch + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        long valeria = count("SELECT id FROM student WHERE dni = '70011223'");
        assertThat(count("SELECT count(*) FROM student")).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM student_stage WHERE stage_id = ?", stage)).isEqualTo(3);
        assertThat(count("SELECT career_id FROM student_stage WHERE student_id = ?", valeria))
                .isEqualTo(count("SELECT id FROM career WHERE name = 'Letras'"));
        assertThat(count("SELECT tutor_id FROM tutor_assignment ta JOIN student_stage ss ON ss.id = ta.student_stage_id "
                + "WHERE ss.student_id = ? AND ta.valid_to IS NULL", valeria)).isEqualTo(silvia);
        // the names of a registered student are never overwritten (the file says "Lucia")
        assertThat(jdbc.queryForObject("SELECT first_name FROM student WHERE id = ?", String.class, lucia)).isEqualTo("Lucía");
        assertThat(output.getOut()).contains("Student import confirmed: batchId=" + batch
                + " created=1 updated=2 enrolled=3 tutorsAssigned=1 rows=3");

        as("ADMIN", post(BATCHES + "/" + batch + "/confirm"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta importación ya fue confirmada o descartada: ya no se puede cambiar."));
    }

    @Test
    void withoutAStageStudentsAreOnlyRegistered() throws Exception {
        long batch = id(uploaded(xlsx(row("Valeria", "Díaz", null, null, null, null, null, "Letras", "Silvia")), null));

        as("TUTOR", post(BATCHES + "/" + batch + "/confirm")).andExpect(status().isOk());

        assertThat(count("SELECT count(*) FROM student")).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM student_stage")).isZero();
    }

    @Test
    void aTutorConfirmingAlsoAssignsTheTutorsOfTheFile() throws Exception {
        long batch = id(uploaded(xlsx(row("Valeria", "Díaz", null, null, null, null, null, null, "Silvia")), stage));

        as("TUTOR", post(BATCHES + "/" + batch + "/confirm")).andExpect(status().isOk());

        assertThat(count("SELECT count(*) FROM tutor_assignment WHERE tutor_id = ?", silvia)).isEqualTo(1);
    }

    @Test
    void anInactiveStudentGoesToAPersonWhenTheBatchEnrolls() throws Exception {
        long retired = student("Ana", "Zapata", null, "70099887", false);
        String json = uploaded(xlsx(row("Ana", "Zapata", null, "70099887")), stage);
        long batch = id(json);

        assertThat(JsonPath.<String>read(json, "$.rows[0].status")).isEqualTo("UNRESOLVED");
        assertThat(JsonPath.<String>read(json, "$.rows[0].notice")).contains("dado de baja");
        resolve("ADMIN", batch, rowId(json, 2), "MATCH", retired)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este alumno está dado de baja: reactívalo antes de matricularlo, o ignora la fila."));
        resolve("ADMIN", batch, rowId(json, 2), "IGNORE", null).andExpect(status().isOk());
        as("ADMIN", post(BATCHES + "/" + batch + "/confirm")).andExpect(status().isOk());

        assertThat(count("SELECT count(*) FROM student_stage WHERE student_id = ?", retired)).isZero();
    }

    @Test
    void uploadingTheSameFileAgainUpdatesInsteadOfAskingOrDuplicating(CapturedOutput output) throws Exception {
        byte[] first = xlsx(
                row("Lucía", "Quispe", "Mamani", "72345678", null, null, null, "Letras", "Silvia"),
                row("Mateo", "Rojas"),
                row("Valeria", "Díaz", "Soto", "70011223"));
        as("ADMIN", post(BATCHES + "/" + id(uploaded(first, stage)) + "/confirm")).andExpect(status().isOk());
        // a name no other test gives a tutor: users created elsewhere are never deleted, and a
        // first name shared by two active tutors matches none (the classes run in any order)
        long rebeca = jdbc.queryForObject("INSERT INTO app_user (email, first_name, last_name_pat) "
                + "VALUES ('rebeca.soto@prueba.pe', 'Rebeca', 'Soto') RETURNING id", Long.class);
        jdbc.update("INSERT INTO user_role (user_id, role_id) SELECT ?, id FROM role WHERE name = 'TUTOR'", rebeca);

        // the same students, with data corrected or added: nobody needs a person
        String json = uploaded(xlsx(
                row("LUCIA", "QUISPE", "MAMANI", "72345678", "lucia@correo.pe", "987654321", "17/05/2009", "Ciencias", "Rebeca"),
                row("mateo", "ROJAS", null, "70055667"),
                row("Valeria", "Díaz", "Soto", "70011223", null, null, null, "Medicina", "Pedro")), stage);
        long batch = id(json);
        assertThat(JsonPath.<List<String>>read(json, "$.rows[*].status"))
                .containsExactly("AUTO_MATCHED", "AUTO_MATCHED", "AUTO_MATCHED");
        assertThat(JsonPath.<Integer>read(json, "$.unresolvedRows")).isZero();
        assertThat(JsonPath.<String>read(json, "$.rows[0].notice")).contains("Ya está matriculado en esta etapa");

        as("TUTOR", post(BATCHES + "/" + batch + "/confirm")).andExpect(status().isOk());

        try {
            // nobody duplicated, nobody enrolled twice
            assertThat(count("SELECT count(*) FROM student")).isEqualTo(3);
            assertThat(count("SELECT count(*) FROM student_stage")).isEqualTo(3);
            // contact data is updated; an empty cell erases nothing; names stay as registered
            assertThat(jdbc.queryForMap("SELECT first_name, email, phone, birth_date::text AS born FROM student WHERE id = ?", lucia))
                    .containsEntry("first_name", "Lucía").containsEntry("email", "lucia@correo.pe")
                    .containsEntry("phone", "987654321").containsEntry("born", "2009-05-17");
            // a student without DNI takes the one the file brings
            assertThat(jdbc.queryForObject("SELECT dni FROM student WHERE id = ?", String.class, mateo)).isEqualTo("70055667");
            // the enrollment takes the new career and tutor, keeping the tutor history
            assertThat(count("SELECT career_id FROM student_stage WHERE student_id = ?", lucia))
                    .isEqualTo(count("SELECT id FROM career WHERE name = 'Ciencias'"));
            assertThat(count("SELECT tutor_id FROM tutor_assignment ta JOIN student_stage ss ON ss.id = ta.student_stage_id "
                    + "WHERE ss.student_id = ? AND ta.valid_to IS NULL", lucia)).isEqualTo(rebeca);
            assertThat(count("SELECT count(*) FROM tutor_assignment ta JOIN student_stage ss ON ss.id = ta.student_stage_id "
                    + "WHERE ss.student_id = ?", lucia)).isEqualTo(2);
            assertThat(output.getOut()).contains("Student import confirmed: batchId=" + batch
                    + " created=0 updated=3 enrolled=0 tutorsAssigned=1 rows=3");
        } finally {
            jdbc.update("DELETE FROM tutor_assignment");
            jdbc.update("DELETE FROM user_role WHERE user_id = ?", rebeca);
            jdbc.update("DELETE FROM app_user WHERE id = ?", rebeca);
        }
    }

    @Test
    void theSameNameWithAnotherDniOrSharedByTwoStudentsGoesToAPerson() throws Exception {
        student("Mateo", "Rojas", null, "70000001", true); // a second Mateo Rojas
        String json = uploaded(xlsx(
                row("Lucía", "Quispe", "Mamani", "72345679"), // one digit off: a typo, not a new student
                row("Mateo", "Rojas"), // two students with this name
                row("Lucio", "Quispe", "Mamani", "70000002")), null); // similar name, another DNI: someone else

        assertThat(JsonPath.<List<String>>read(json, "$.rows[*].status"))
                .containsExactly("UNRESOLVED", "UNRESOLVED", "NEW_STUDENT");
        assertThat(JsonPath.<List<Integer>>read(json, "$.rows[0].candidates[*].id")).containsExactly((int) lucia);
        assertThat(JsonPath.<List<Integer>>read(json, "$.rows[1].candidates[*].id")).hasSize(2);
    }

    @Test
    void aRowCannotBeNewWithADniThatExists() throws Exception {
        String json = uploaded(xlsx(row("Lucía", "Quispe", null, "72345678")), null);

        resolve("ADMIN", id(json), rowId(json, 2), "NEW", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un alumno con ese DNI: empareja la fila con él."));
        resolve("ADMIN", id(json), rowId(json, 2), "MATCH", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Elige el alumno con el que se empareja la fila."));
    }

    @Test
    void aDiscardedBatchCanNoLongerChange() throws Exception {
        String json = uploaded(xlsx(row("Mateo", "Roja")), null);
        long batch = id(json);

        as("ADMIN", post(BATCHES + "/" + batch + "/discard")).andExpect(status().isNoContent());
        as("ADMIN", get(BATCHES + "/" + batch)).andExpect(jsonPath("$.status").value("DISCARDED"));
        resolve("ADMIN", batch, rowId(json, 2), "NEW", null).andExpect(status().isConflict());
        assertThat(count("SELECT count(*) FROM student")).isEqualTo(2);
    }

    @Test
    void pendingBatchesAreListedNewestFirstWithoutTheirRows() throws Exception {
        long first = id(uploaded(xlsx(row("Mateo", "Roja")), null));
        long second = id(uploaded(xlsx(row("Valeria", "Díaz")), stage));
        as("ADMIN", post(BATCHES + "/" + second + "/confirm")).andExpect(status().isOk());

        as("TUTOR", get(BATCHES).param("status", "PENDING_REVIEW"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(first))
                .andExpect(jsonPath("$[0].unresolvedRows").value(1))
                .andExpect(jsonPath("$[0].rows.length()").value(0));
        as("TUTOR", get(BATCHES)).andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.contains((int) second, (int) first)));
    }

    @Test
    void anInvalidFileIsRejectedWholeCitingRowsNeverData() throws Exception {
        upload("ADMIN", xlsx(
                row(null, "Díaz"),
                row("Valeria", "Díaz", null, "7001-1223"),
                row("Ana", "Soto", null, "70099887", "no-es-correo", null, "31/02/2009"),
                row("Ana", "Soto", null, "70099887")), stage)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("fila 2: Ingresa los nombres del alumno.")))
                .andExpect(jsonPath("$.detail").value(containsString("fila 3: El DNI debe tener entre 8 y 12 letras o números.")))
                .andExpect(jsonPath("$.detail").value(containsString("fila 4: El correo no tiene un formato válido.")))
                .andExpect(jsonPath("$.detail").value(containsString("fila 4: La fecha de nacimiento no tiene un formato válido")))
                .andExpect(jsonPath("$.detail").value(containsString("fila 5: El DNI se repite en la fila 4.")))
                .andExpect(jsonPath("$.detail").value(not(containsString("7001-1223"))));
        assertThat(count("SELECT count(*) FROM import_batch")).isZero();
    }

    @Test
    void aFileWithOtherColumnsOrNoStudentsIsRejected() throws Exception {
        String[] extra = Arrays.copyOf(HEADERS, 10);
        extra[9] = "Colegio";
        upload("ADMIN", xlsxWithHeaders(extra, row("Valeria", "Díaz")), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("no tiene las columnas de la plantilla")));
        upload("ADMIN", xlsxWithHeaders(Arrays.copyOf(HEADERS, 7), row("Valeria", "Díaz")), null).andExpect(status().isBadRequest());
        upload("ADMIN", xlsx(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El archivo no tiene alumnos: llena la plantilla desde la segunda fila."));
        upload("ADMIN", "no es un excel".getBytes(), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("No pudimos leer el archivo")));
        as("ADMIN", multipart(IMPORTS)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Adjunta el archivo."));
    }

    @Test
    void theStageMustExistAndBeOfAnOpenCycle() throws Exception {
        upload("ADMIN", xlsx(row("Valeria", "Díaz")), 999999L).andExpect(status().isNotFound());
        jdbc.update("UPDATE cycle SET status = 'CLOSED'");
        upload("ADMIN", xlsx(row("Valeria", "Díaz")), stage)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No se puede matricular en una etapa de un ciclo cerrado."));
    }

    @Test
    void aRowCanBeResolvedWithAnyStudentFoundByNameOrDni() throws Exception {
        long batch = id(uploaded(xlsx(row("Mateo", "Roja")), null));
        String search = BATCHES + "/" + batch + "/student-search";

        // a tutor finds any student here, not only their own
        as("TUTOR", get(search).param("query", "quispe luc"))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.contains((int) lucia)))
                .andExpect(jsonPath("$[0].fullName").value("Quispe Mamani, Lucía"));
        as("TUTOR", get(search).param("query", "7234")).andExpect(jsonPath("$.length()").value(1));
        as("TUTOR", get(search).param("query", "m")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aCoordinatorDoesNotImportStudents() throws Exception {
        upload("COORDINADOR", xlsx(row("Valeria", "Díaz")), null).andExpect(status().isForbidden());
        // the review queue only shows them their exam results batches
        as("COORDINADOR", get(BATCHES)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        as("COORDINADOR", get(IMPORTS + "/template")).andExpect(status().isForbidden());
    }
}
