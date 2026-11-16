package pe.com.edu.prisma.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
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
 * Importing an exam's results from the academy's Excel (REQ-EXAMS-02,
 * agreements of 2026-11-16). The EVA sheets are built here with the layout
 * of the real files (title in B5, sub-headers in row 11, students from row
 * 12); every person is invented. Exam types come from the seed: 1 Ciencias
 * (4 Matemática courses), 2 Letras (5), 3 Arquitectura (3); so does the
 * tutor Silvia Valle Gómez, whom the TUTOR(A) column names as "SILVIA".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class ExamResultImportApiIT {

    private static final String BATCHES = "/api/v1/import-batches";
    private static final long CIENCIAS = 1;
    private static final long LETRAS = 2;
    private static final String LETRAS_TITLE = "EXAMEN DE REPASO N°1\nTALENTO 2027.1\nÁREA:  LETRAS - ESTUDIOS INTERDISCIPLINARIOS";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long me;
    private long stage;
    private long letras;
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
        long mockExam = mockExam(stage, "Repaso 1");
        letras = exam(mockExam, LETRAS);
        lucia = student("Lucía", "Quispe", "Mamani", "72345678", true);
        mateo = student("Mateo", "Rojas", null, null, true);
        enroll(lucia, stage, true);
    }

    @AfterEach
    void emptyTables() {
        jdbc.update("DELETE FROM import_row");
        jdbc.update("DELETE FROM import_batch");
        jdbc.update("DELETE FROM exam_result");
        jdbc.update("DELETE FROM exam");
        jdbc.update("DELETE FROM mock_exam");
        jdbc.update("DELETE FROM tutor_assignment");
        jdbc.update("DELETE FROM student_stage");
        jdbc.update("DELETE FROM student");
        jdbc.update("DELETE FROM stage");
        jdbc.update("DELETE FROM cycle");
    }

    private long mockExam(long stageId, String name) {
        return jdbc.queryForObject("INSERT INTO mock_exam (stage_id, name, exam_date) VALUES (?, ?, '2026-03-14') "
                + "RETURNING id", Long.class, stageId, name);
    }

    private long exam(long mockExamId, long examTypeId) {
        return jdbc.queryForObject("INSERT INTO exam (mock_exam_id, exam_type_id) VALUES (?, ?) RETURNING id",
                Long.class, mockExamId, examTypeId);
    }

    private long student(String firstName, String lastNamePat, String lastNameMat, String dni, boolean active) {
        return jdbc.queryForObject("INSERT INTO student (first_name, last_name_pat, last_name_mat, dni, active) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, firstName, lastNamePat, lastNameMat, dni, active);
    }

    private void enroll(long studentId, long stageId, boolean active) {
        jdbc.update("INSERT INTO student_stage (student_id, stage_id, active) VALUES (?, ?, ?)", studentId, stageId, active);
    }

    // ---------------------------------------------------------------- EVA sheet

    /** A student row: DNI (text or number), name and the B/M/BL counts of each course, then the scores. */
    private record Line(Object dni, Object name, int[] counts, Double reading, Double math, Double total, String tutor) {

        /** The same row with another text in its TUTOR(A) cell (null = empty). */
        Line tutor(String text) {
            return new Line(dni, name, counts, reading, math, total, text);
        }
    }

    private static Line line(Object dni, String name, int[] counts, double reading, double math, double total) {
        return new Line(dni, name, counts, reading, math, total, "SILVIA"); // the seeded tutor, as the academy writes her
    }

    /** Lectura then {@code mathCourses} courses, all with the same counts. */
    private static int[] counts(int mathCourses, int correct, int wrong, int blank) {
        int[] counts = new int[3 * (1 + mathCourses)];
        for (int i = 0; i < counts.length; i += 3) {
            counts[i] = correct;
            counts[i + 1] = wrong;
            counts[i + 2] = blank;
        }
        return counts;
    }

    /** A row that is not a student: only these cells, by column index. */
    private record Junk(Object[] cells) {
    }

    private static byte[] eva(String title, int mathCourses, Object... rows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.createSheet("LISTA DE ALUMNOS");
            Sheet sheet = workbook.createSheet("EVA");
            sheet.createRow(4).createCell(1).setCellValue(title);
            Row titles = sheet.createRow(8);
            set(titles, 1, "ORDEN DE MERITO");
            set(titles, 2, "DNI");
            set(titles, 3, "ALUMNO");
            Row sub = sheet.createRow(10);
            set(sub, 3, "APELLIDOS Y NOMBRES");
            int c = 7;
            c = triple(sub, c, "BL");          // Lectura (course)
            c = triple(sub, c, "PUNTAJE") + 1; // Lectura (area), then a hidden column
            for (int i = 0; i < mathCourses; i++) {
                c = triple(sub, c, "BL");
            }
            int mathArea = c;
            c = triple(sub, c, "PUNTAJE") + 2; // Matemática (area), then two hidden columns
            int total = c;
            c = triple(sub, c, "PUNTAJE");
            set(titles, c, "CONDICIÓN");
            set(titles, c + 1, "TUTOR(A)");

            int r = 11;
            int merit = 1;
            for (Object item : rows) {
                Row row = sheet.createRow(r++);
                if (item instanceof Junk junk) {
                    for (int i = 0; i < junk.cells().length; i++) {
                        set(row, i, junk.cells()[i]);
                    }
                    continue;
                }
                Line line = (Line) item;
                set(row, 1, merit++);
                set(row, 2, line.dni());
                set(row, 3, line.name());
                int[] counts = line.counts();
                for (int i = 0; i < 3; i++) {
                    set(row, 7 + i, counts[i]);
                }
                for (int i = 3; i < counts.length; i++) {
                    set(row, 14 + i - 3, counts[i]);
                }
                set(row, 12, line.reading());
                set(row, mathArea + 2, line.math());
                set(row, total + 2, line.total());
                set(row, c + 1, line.tutor());
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static int triple(Row row, int column, String third) {
        set(row, column, "B");
        set(row, column + 1, "M");
        set(row, column + 2, third);
        return column + 3;
    }

    private static void set(Row row, int column, Object value) {
        if (value instanceof String text) {
            row.createCell(column).setCellValue(text);
        } else if (value instanceof Number number) {
            row.createCell(column).setCellValue(number.doubleValue());
        }
    }

    /** Lucía by DNI (enrolled), Mateo by name (not enrolled), someone unknown, someone absent and junk. */
    private static byte[] letrasFile() throws Exception {
        return eva(LETRAS_TITLE, 5,
                line("72345678", "QUISPE MAMANI LUCIA", counts(5, 4, 1, 0), 787.5, 819.4, 722.76),
                line(null, "ROJAS MATEO", counts(5, 3, 2, 1), 740.9, 741.6, 673.0),
                line("71112223", "TORRES VEGA ANA SOFIA", counts(5, 2, 0, 3), 677.3, 692.3, 627.84),
                line("70000001", "PAREDES LUNA DIEGO", counts(5, 0, 0, 4), 0.0, 0.0, 80.0),
                new Junk(new Object[] {null, 18, 50, 18}),             // sorting leftovers: numbers
                new Junk(new Object[] {null, 19, 72345678, null}),     // a real DNI without a name
                new Junk(new Object[] {null, "LEC", null, null, null, null, null, 16.6})); // the footer
    }

    // ---------------------------------------------------------------- requests

    private ResultActions as(String role, AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
        AuthenticatedUser principal = () -> me;
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return mockMvc.perform(request.with(authentication(auth)).with(csrf()));
    }

    private ResultActions upload(String role, long examId, byte[] file) throws Exception {
        return as(role, multipart("/api/v1/exams/" + examId + "/results/import").file(new MockMultipartFile("file",
                "GENERALES LETRAS.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file)));
    }

    private String uploaded(String role, byte[] file) throws Exception {
        return upload(role, letras, file).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private static long rowId(String json, int rowNumber) {
        List<Integer> ids = JsonPath.read(json, "$.rows[?(@.rowNumber == " + rowNumber + ")].id");
        return ids.get(0);
    }

    private ResultActions resolve(String role, long batch, long row, String body) throws Exception {
        return as(role, put(BATCHES + "/" + batch + "/rows/" + row).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions confirm(String role, long batch) throws Exception {
        return as(role, post(BATCHES + "/" + batch + "/confirm"));
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    // ---------------------------------------------------------------- tests

    @Test
    void uploadingReadsTheStudentsSkipsTheRestAndWritesNothingReal(CapturedOutput output) throws Exception {
        String json = uploaded("TUTOR", letrasFile());
        long batch = id(json);

        as("TUTOR", get(BATCHES + "/" + batch))
                .andExpect(jsonPath("$.type").value("EXAM_RESULTS"))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.examId").value(letras))
                .andExpect(jsonPath("$.examLabel").value("Repaso 1 · Letras"))
                .andExpect(jsonPath("$.stageLabel").value("2026-I · Regular"))
                .andExpect(jsonPath("$.totalRows").value(4))
                .andExpect(jsonPath("$.skippedRows").value(3))
                .andExpect(jsonPath("$.matchedRows").value(2))
                .andExpect(jsonPath("$.unresolvedRows").value(2))
                // Lucía by DNI, already enrolled
                .andExpect(jsonPath("$.rows[0].rowNumber").value(12))
                .andExpect(jsonPath("$.rows[0].status").value("AUTO_MATCHED"))
                .andExpect(jsonPath("$.rows[0].student.id").value(lucia))
                .andExpect(jsonPath("$.rows[0].enrolls").value(false))
                .andExpect(jsonPath("$.rows[0].tutorText").value("SILVIA"))
                .andExpect(jsonPath("$.rows[0].tutorName").value("Valle Gómez, Silvia"))
                .andExpect(jsonPath("$.rows[0].result.attended").value(true))
                .andExpect(jsonPath("$.rows[0].result.totalCorrect").value(24))
                .andExpect(jsonPath("$.rows[0].result.totalWrong").value(6))
                .andExpect(jsonPath("$.rows[0].result.totalScore").value(722.76))
                .andExpect(jsonPath("$.rows[0].result.areas[0].areaName").value("Lectura"))
                .andExpect(jsonPath("$.rows[0].result.areas[0].score").value(787.5))
                .andExpect(jsonPath("$.rows[0].result.areas[1].score").value(819.4))
                // Mateo by his exact name: not enrolled, so confirming enrolls him
                .andExpect(jsonPath("$.rows[1].status").value("AUTO_MATCHED"))
                .andExpect(jsonPath("$.rows[1].student.id").value(mateo))
                .andExpect(jsonPath("$.rows[1].enrolls").value(true))
                .andExpect(jsonPath("$.rows[1].notice").value(containsString("se matriculará al confirmar en Letras")))
                // nobody registered: a person decides
                .andExpect(jsonPath("$.rows[2].status").value("UNRESOLVED"))
                .andExpect(jsonPath("$.rows[2].fullName").value("TORRES VEGA ANA SOFIA"))
                .andExpect(jsonPath("$.rows[2].candidates.length()").value(0))
                // 0 correct and 0 wrong everywhere: did not attend
                .andExpect(jsonPath("$.rows[3].result.attended").value(false));

        assertThat(count("SELECT count(*) FROM exam_result")).isZero();
        assertThat(count("SELECT count(*) FROM student_stage WHERE student_id = ?", mateo)).isZero();
        assertThat(output.getAll()).contains("Exam results import uploaded: batchId=" + batch)
                .doesNotContain("QUISPE").doesNotContain("72345678").doesNotContain("722.76");
    }

    @Test
    void confirmingEnrollsRegistersAndSavesTheResultsAsTheyCome(CapturedOutput output) throws Exception {
        String json = uploaded("ADMIN", letrasFile());
        long batch = id(json);
        resolve("ADMIN", batch, rowId(json, 14),
                "{\"action\":\"NEW\",\"firstName\":\"Ana Sofía\",\"lastNamePat\":\"Torres\",\"lastNameMat\":\"Vega\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[2].status").value("NEW_STUDENT"))
                .andExpect(jsonPath("$.rows[2].enrolls").value(true))
                .andExpect(jsonPath("$.rows[2].newStudent.firstName").value("Ana Sofía"));
        resolve("ADMIN", batch, rowId(json, 15), "{\"action\":\"IGNORE\"}").andExpect(status().isOk());

        confirm("ADMIN", batch).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertThat(count("SELECT count(*) FROM exam_result WHERE exam_id = ?", letras)).isEqualTo(3);
        long ana = count("SELECT id FROM student WHERE dni = '71112223' AND first_name = 'Ana Sofía' "
                + "AND last_name_pat = 'Torres' AND last_name_mat = 'Vega'");
        // enrolled by the import: in the career named as the exam type (Lucía was already enrolled)
        for (long student : List.of(mateo, ana)) {
            assertThat(count("SELECT count(*) FROM student_stage s JOIN career c ON c.id = s.career_id "
                    + "WHERE s.student_id = ? AND s.stage_id = ? AND s.active AND c.name = 'Letras'", student, stage))
                    .isEqualTo(1);
        }
        assertThat(count("SELECT count(*) FROM student_stage WHERE student_id = ? AND career_id IS NULL", lucia))
                .isEqualTo(1);
        // nobody had a tutor: the three get the one the TUTOR column names, also Lucía who was already enrolled
        for (long student : List.of(lucia, mateo, ana)) {
            assertThat(count("SELECT count(*) FROM tutor_assignment a JOIN student_stage s ON s.id = a.student_stage_id "
                    + "WHERE s.student_id = ? AND a.tutor_id = ? AND a.valid_to IS NULL", student, silvia)).isEqualTo(1);
        }

        long result = count("SELECT r.id FROM exam_result r JOIN student_stage s ON s.id = r.student_stage_id "
                + "WHERE s.student_id = ?", lucia);
        var saved = jdbc.queryForMap("SELECT total_correct, total_wrong, total_blank, total_score, attended "
                + "FROM exam_result WHERE id = ?", result);
        assertThat(saved).containsEntry("total_correct", 24).containsEntry("total_wrong", 6)
                .containsEntry("total_blank", 0).containsEntry("attended", true);
        assertThat((BigDecimal) saved.get("total_score")).isEqualByComparingTo("722.76");
        assertThat(jdbc.queryForList("SELECT course_id FROM exam_course_result WHERE exam_result_id = ? "
                + "AND correct = 4 AND wrong = 1 AND blank = 0 ORDER BY course_id", Long.class, result))
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
        assertThat(jdbc.queryForList("SELECT score FROM exam_area_result a JOIN area ON area.id = a.area_id "
                + "WHERE exam_result_id = ? ORDER BY area.id", BigDecimal.class, result))
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("787.5"), new BigDecimal("819.4"));

        as("COORDINADOR", get("/api/v1/mock-exams").param("stageId", String.valueOf(stage)))
                .andExpect(jsonPath("$[0].exams[0].hasResults").value(true));
        assertThat(output.getAll()).contains("Exam results import confirmed: batchId=" + batch + " examId=" + letras
                + " results=3 studentsCreated=1 enrolled=2 tutorsAssigned=3");

        // loaded once: never again, neither uploading nor confirming another batch
        upload("ADMIN", letras, letrasFile()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("ya tiene resultados")));
    }

    @Test
    void anotherBatchOfTheSameExamCannotBeConfirmedOnceItHasResults() throws Exception {
        byte[] file = eva(LETRAS_TITLE, 5, line("72345678", "QUISPE MAMANI LUCIA", counts(5, 1, 0, 0), 1, 1, 1));
        long first = id(uploaded("ADMIN", file));
        long second = id(uploaded("ADMIN", file));
        confirm("ADMIN", first).andExpect(status().isOk());
        confirm("ADMIN", second).andExpect(status().isConflict());
        assertThat(count("SELECT count(*) FROM exam_result")).isEqualTo(1);
    }

    @Test
    void theDniIsNormalizedAndAnAbsentStudentIsKept() throws Exception {
        byte[] file = eva(LETRAS_TITLE, 5, line(72345678.0, "Quispe Mamani Lucía", counts(5, 0, 0, 2), 0, 0, 80));
        String json = uploaded("ADMIN", file);
        assertThat(JsonPath.<Integer>read(json, "$.rows[0].student.id")).isEqualTo((int) lucia);
        confirm("ADMIN", id(json)).andExpect(status().isOk());
        assertThat(count("SELECT count(*) FROM exam_result WHERE NOT attended")).isEqualTo(1);
    }

    @Test
    void theWrongFileIsRejectedWholeCitingRowsNeverData() throws Exception {
        // the title names another exam type
        upload("ADMIN", letras, eva("ÁREA: CIENCIAS - BIOMÉDICA", 5, line(null, "ROJAS MATEO", counts(5, 1, 0, 0), 1, 1, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El archivo es de Ciencias y el examen es de Letras: "
                        + "sube el archivo de Letras."));
        // four Matemática courses (Ciencias) where Letras has five
        upload("ADMIN", letras, eva("RESULTADOS", 4, line(null, "ROJAS MATEO", counts(4, 1, 0, 0), 1, 1, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("no tiene las columnas de cursos de un examen de Letras")));
        // the same DNI or name twice among the students
        upload("ADMIN", letras, eva(LETRAS_TITLE, 5,
                line("72345678", "QUISPE MAMANI LUCIA", counts(5, 1, 0, 0), 1, 1, 1),
                line("72345678", "ROJAS MATEO", counts(5, 1, 0, 0), 1, 1, 1),
                line(null, "rojas  mateo", counts(5, 1, 0, 0), 1, 1, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Corrige el archivo y vuelve a subirlo: fila 13: el DNI se repite "
                        + "en la fila 12. · fila 14: el nombre se repite en la fila 13."));
        // no students at all, or no EVA sheet
        upload("ADMIN", letras, eva(LETRAS_TITLE, 5, new Junk(new Object[] {null, 1, 2, 3})))
                .andExpect(status().isBadRequest());
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.createSheet("Hoja1");
            workbook.write(out);
            upload("ADMIN", letras, out.toByteArray()).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(containsString("hoja EVA")));
        }
        assertThat(count("SELECT count(*) FROM import_batch")).isZero();
    }

    @Test
    void aCienciasFileFitsACienciasExam() throws Exception {
        long ciencias = exam(mockExam(stage, "Repaso 2"), CIENCIAS);
        byte[] file = eva("ÁREA:  CIENCIAS - BIOMÉDICA", 4, line("72345678", "QUISPE MAMANI LUCIA", counts(4, 2, 1, 0), 1, 2, 3));
        upload("ADMIN", ciencias, file).andExpect(status().isCreated())
                .andExpect(jsonPath("$.rows[0].result.totalCorrect").value(10));
    }

    @Test
    void onlyExamsOfTheCurrentStageAcceptResults() throws Exception {
        long cycle = count("SELECT cycle_id FROM stage WHERE id = ?", stage);
        // a later stage that already started: Regular is no longer the current one
        jdbc.update("INSERT INTO stage (cycle_id, name, start_date, end_date) VALUES (?, 'Repaso', '2026-07-01', '2026-12-31')", cycle);
        upload("ADMIN", letras, letrasFile()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("etapa vigente")));
        upload("ADMIN", 999_999, letrasFile()).andExpect(status().isNotFound());
    }

    @Test
    void aRowIsResolvedOnlyWithAStudentWhoCanTakeTheResult() throws Exception {
        long withdrawn = student("Rosa", "Huamán", null, "73334445", true);
        enroll(withdrawn, stage, false);
        long inactive = student("Pedro", "Salas", null, "74445556", false);
        String json = uploaded("ADMIN", eva(LETRAS_TITLE, 5,
                line("73334445", "HUAMAN ROSA", counts(5, 1, 0, 0), 1, 1, 1),
                line("74445556", "SALAS PEDRO", counts(5, 1, 0, 0), 1, 1, 1),
                line(null, "QUISPE MAMANI LUCIA", counts(5, 1, 0, 0), 1, 1, 1),
                line("75556667", "NUEVO ALUMNO", counts(5, 1, 0, 0), 1, 1, 1)));
        long batch = id(json);

        // withdrawn from the stage, or inactive and not enrolled: only ignoring the row
        as("ADMIN", get(BATCHES + "/" + batch))
                .andExpect(jsonPath("$.rows[0].status").value("UNRESOLVED"))
                .andExpect(jsonPath("$.rows[0].notice").value(containsString("retirado")))
                .andExpect(jsonPath("$.rows[1].status").value("UNRESOLVED"))
                .andExpect(jsonPath("$.rows[1].notice").value(containsString("dado de baja")))
                .andExpect(jsonPath("$.rows[2].status").value("AUTO_MATCHED"));
        resolve("ADMIN", batch, rowId(json, 12), "{\"action\":\"MATCH\",\"studentId\":" + withdrawn + "}")
                .andExpect(status().isConflict());
        resolve("ADMIN", batch, rowId(json, 13), "{\"action\":\"MATCH\",\"studentId\":" + inactive + "}")
                .andExpect(status().isConflict());
        // a student already in another row
        resolve("ADMIN", batch, rowId(json, 15), "{\"action\":\"MATCH\",\"studentId\":" + lucia + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ese alumno ya está en la fila 14: ignora una de las dos."));
        // a new student needs its names split by hand
        resolve("ADMIN", batch, rowId(json, 15), "{\"action\":\"NEW\",\"lastNamePat\":\"Nuevo\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Ingresa los nombres del alumno."));
        // and its DNI can't be someone else's
        resolve("ADMIN", batch, rowId(json, 12),
                "{\"action\":\"NEW\",\"firstName\":\"Rosa\",\"lastNamePat\":\"Huamán\"}")
                .andExpect(status().isConflict());

        confirm("ADMIN", batch).andExpect(status().isConflict()); // rows 12 and 13 still unresolved
        resolve("ADMIN", batch, rowId(json, 12), "{\"action\":\"IGNORE\"}").andExpect(status().isOk());
        resolve("ADMIN", batch, rowId(json, 13), "{\"action\":\"IGNORE\"}").andExpect(status().isOk());
        resolve("ADMIN", batch, rowId(json, 15), "{\"action\":\"IGNORE\"}").andExpect(status().isOk());
        confirm("ADMIN", batch).andExpect(status().isOk());
        assertThat(count("SELECT count(*) FROM exam_result")).isEqualTo(1);
    }

    @Test
    void aCoordinatorImportsResultsButNotStudents() throws Exception {
        String json = uploaded("COORDINADOR", eva(LETRAS_TITLE, 5,
                line(null, "ROJAS MATEO", counts(5, 1, 0, 0), 1, 1, 1)));
        long students = count("INSERT INTO import_batch (type, file_name, uploaded_by) VALUES ('STUDENTS', 'a.xlsx', ?) "
                + "RETURNING id", me);

        as("COORDINADOR", get(BATCHES)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("EXAM_RESULTS"));
        as("TUTOR", get(BATCHES)).andExpect(jsonPath("$.length()").value(2));
        as("COORDINADOR", get(BATCHES + "/" + students)).andExpect(status().isForbidden());
        as("COORDINADOR", get(BATCHES + "/" + id(json) + "/student-search").param("query", "rojas"))
                .andExpect(jsonPath("$[0].id").value(mateo));

        confirm("COORDINADOR", id(json)).andExpect(status().isOk());
        assertThat(count("SELECT count(*) FROM student_stage WHERE student_id = ?", mateo)).isEqualTo(1);
    }

    @Test
    void withoutAnActiveCareerNamedAsTheTypeTheyAreEnrolledWithoutCareer() throws Exception {
        jdbc.update("UPDATE career SET active = false WHERE name = 'Letras'");
        try {
            String json = uploaded("ADMIN", eva(LETRAS_TITLE, 5, line(null, "ROJAS MATEO", counts(5, 1, 0, 0), 1, 1, 1)));
            assertThat(JsonPath.<String>read(json, "$.rows[0].notice")).contains("sin carrera");
            confirm("ADMIN", id(json)).andExpect(status().isOk());
            assertThat(count("SELECT count(*) FROM student_stage WHERE student_id = ? AND career_id IS NULL", mateo))
                    .isEqualTo(1);
        } finally {
            jdbc.update("UPDATE career SET active = true WHERE name = 'Letras'");
        }
    }

    @Test
    void theFileTutorNeverReplacesOneAndAnUnknownNameLeavesWithoutTutor() throws Exception {
        // Lucía already has a tutor in the stage (any user will do here)
        jdbc.update("INSERT INTO tutor_assignment (student_stage_id, tutor_id) "
                + "SELECT id, ? FROM student_stage WHERE student_id = ?", me, lucia);
        String json = uploaded("ADMIN", eva(LETRAS_TITLE, 5,
                line("72345678", "QUISPE MAMANI LUCIA", counts(5, 1, 0, 0), 1, 1, 1),
                line(null, "ROJAS MATEO", counts(5, 1, 0, 0), 1, 1, 1).tutor("XIMENA"),
                line("70000002", "PAREDES LUNA DIEGO", counts(5, 1, 0, 0), 1, 1, 1).tutor(null)));
        long batch = id(json);

        as("ADMIN", get(BATCHES + "/" + batch))
                // the tutor she has, not the file's
                .andExpect(jsonPath("$.rows[0].tutorText").value("SILVIA"))
                .andExpect(jsonPath("$.rows[0].tutorName").value("Palacios Coronado, Antony"))
                // nobody is called Ximena: without a tutor, but not to review
                .andExpect(jsonPath("$.rows[1].status").value("AUTO_MATCHED"))
                .andExpect(jsonPath("$.rows[1].tutorText").value("XIMENA"))
                .andExpect(jsonPath("$.rows[1].tutorName").doesNotExist());
        resolve("ADMIN", batch, rowId(json, 14),
                "{\"action\":\"NEW\",\"firstName\":\"Diego\",\"lastNamePat\":\"Paredes\",\"lastNameMat\":\"Luna\"}")
                .andExpect(status().isOk());
        confirm("ADMIN", batch).andExpect(status().isOk());

        assertThat(count("SELECT count(*) FROM tutor_assignment")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM tutor_assignment WHERE tutor_id = ? AND valid_to IS NULL", me)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM exam_result")).isEqualTo(3);
    }

    @Test
    void discardingLeavesTheExamFreeToLoad() throws Exception {
        long batch = id(uploaded("ADMIN", letrasFile()));
        as("TUTOR", post(BATCHES + "/" + batch + "/discard")).andExpect(status().isNoContent());
        confirm("ADMIN", batch).andExpect(status().isConflict());
        upload("ADMIN", letras, letrasFile()).andExpect(status().isCreated());
    }
}
