package pe.com.edu.prisma.imports.impl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import pe.com.edu.prisma.catalog.Area;
import pe.com.edu.prisma.catalog.Course;
import pe.com.edu.prisma.catalog.ExamType;
import pe.com.edu.prisma.common.exception.InvalidRequestException;
import pe.com.edu.prisma.imports.ResultRowPayload.AreaScore;
import pe.com.edu.prisma.imports.ResultRowPayload.CourseCount;

/**
 * Reads the academy's results file (sheet EVA, CLAUDE.md §4.4). The title is
 * in B5, the sub-headers in row 11 and the students from row 12: DNI in C,
 * "APELLIDOS Y NOMBRES" in D and, from H on, for each area its courses (B, M,
 * BL) followed by the area (B, M, PUNTAJE), and at the end the total (B, M,
 * PUNTAJE). Columns are recognized by that position pattern, never by the
 * course titles (they come with typos, and Arquitectura's "Geometría y
 * Trigonometría" is only Geometría). After the total come CONDICIÓN and
 * TUTOR(A): the tutor's first name, read as text. Merit, header date,
 * CONDICIÓN and the school data are ignored.
 *
 * Error messages only cite row numbers: never the data itself.
 */
@Component
public class ResultSheet {

    static final String SHEET = "EVA";
    private static final int TITLE_ROW = 4;      // B5
    private static final int TITLE_COLUMN = 1;
    private static final int SUBHEADER_ROW = 10; // row 11
    private static final int FIRST_DATA_ROW = 11; // row 12
    private static final int MERIT_COLUMN = 1;   // B
    private static final int DNI_COLUMN = 2;     // C
    private static final int NAME_COLUMN = 3;    // D
    private static final int FIRST_SCORE_COLUMN = 7; // H
    private static final int TUTOR_AFTER_TOTAL = 4;  // total B, M, PUNTAJE, CONDICIÓN, then TUTOR(A)
    private static final int MAX_ERRORS_SHOWN = 10;
    private static final Pattern VALID_DNI = Pattern.compile("[A-Z0-9]{8,12}");
    private static final Pattern HAS_LETTER = Pattern.compile("\\p{L}");

    /** A student row: as read, before matching. */
    public record ParsedRow(int rowNumber, String dni, String fullName, List<CourseCount> courses,
            List<AreaScore> areas, BigDecimal totalScore, boolean attended, String tutorText) {
    }

    /** The student rows and how many other rows were skipped (no name: leftovers, footers). */
    public record Parsed(List<ParsedRow> rows, int skipped) {
    }

    // where a course's B/M/BL or an area's B/M/PUNTAJE start
    private record Layout(Map<Course, Integer> courses, Map<Area, Integer> areas, int total) {
    }

    /**
     * The file's rows, or an {@link InvalidRequestException} when it is not
     * a results file of {@code type}: another title, other course columns,
     * or a DNI or name repeated among the students.
     */
    public Parsed read(InputStream file, ExamType type, List<String> otherTypeNames) {
        Sheet sheet = openEva(file);
        checkTitle(sheet, type, otherTypeNames);
        Layout layout = layout(sheet, type);

        List<ParsedRow> rows = new ArrayList<>();
        int skipped = 0;
        for (int i = FIRST_DATA_ROW; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlank(row)) {
                continue;
            }
            String name = studentName(row.getCell(NAME_COLUMN));
            if (name == null) {
                skipped++; // not a student: sorting leftovers, numbers, the averages at the bottom
                continue;
            }
            rows.add(toRow(i + 1, row, name, layout));
        }
        if (rows.isEmpty()) {
            throw new InvalidRequestException("El archivo no tiene resultados de alumnos desde la fila 12 de la hoja EVA.");
        }
        rejectRepeated(rows);
        return new Parsed(rows, skipped);
    }

    private static Sheet openEva(InputStream file) {
        Workbook workbook;
        try {
            workbook = WorkbookFactory.create(file);
        } catch (IOException | RuntimeException e) {
            throw new InvalidRequestException("No pudimos leer el archivo: sube el Excel de resultados (.xlsx).");
        }
        for (Sheet sheet : workbook) {
            if (sheet.getSheetName().trim().equalsIgnoreCase(SHEET)) {
                return sheet;
            }
        }
        throw new InvalidRequestException("El archivo no tiene la hoja EVA: sube el Excel de resultados de la academia.");
    }

    /** The title may name no exam type; naming another one (and not this one) is the wrong file. */
    private static void checkTitle(Sheet sheet, ExamType type, List<String> otherTypeNames) {
        String title = text(cell(sheet.getRow(TITLE_ROW), TITLE_COLUMN));
        if (title == null) {
            return;
        }
        String normalized = ImportNames.normalize(title);
        if (mentions(normalized, type.getName())) {
            return;
        }
        otherTypeNames.stream().filter(other -> mentions(normalized, other)).findFirst().ifPresent(other -> {
            throw new InvalidRequestException("El archivo es de " + other + " y el examen es de " + type.getName()
                    + ": sube el archivo de " + type.getName() + ".");
        });
    }

    private static boolean mentions(String normalizedTitle, String typeName) {
        String word = Pattern.quote(ImportNames.normalize(typeName));
        return Pattern.compile("(^|[^\\p{L}])" + word + "($|[^\\p{L}])").matcher(normalizedTitle).find();
    }

    /**
     * Reads row 11 from H on: each "B M BL" is a course and each "B M PUNTAJE"
     * closes an area (and the last one is the total). The exam type's
     * courses, by area and in catalog order, must fill exactly that pattern.
     */
    private static Layout layout(Sheet sheet, ExamType type) {
        Row header = sheet.getRow(SUBHEADER_ROW);
        List<int[]> groups = new ArrayList<>(); // {column, isScore}
        int last = header == null ? -1 : header.getLastCellNum();
        for (int c = FIRST_SCORE_COLUMN; c + 2 < last; c++) {
            String b = token(header, c);
            String m = token(header, c + 1);
            String third = token(header, c + 2);
            if ("b".equals(b) && "m".equals(m) && ("bl".equals(third) || "puntaje".equals(third))) {
                groups.add(new int[] {c, "puntaje".equals(third) ? 1 : 0});
                c += 2;
            }
        }

        Map<Area, List<Course>> byArea = new LinkedHashMap<>();
        type.getCourses().stream()
                .sorted(Comparator.comparing((Course course) -> course.getArea().getId()).thenComparing(Course::getId))
                .forEach(course -> byArea.computeIfAbsent(course.getArea(), a -> new ArrayList<>()).add(course));

        Map<Course, Integer> courses = new LinkedHashMap<>();
        Map<Area, Integer> areas = new LinkedHashMap<>();
        int g = 0;
        boolean fits = true;
        for (Map.Entry<Area, List<Course>> area : byArea.entrySet()) {
            for (Course course : area.getValue()) {
                fits &= g < groups.size() && groups.get(g)[1] == 0;
                if (fits) {
                    courses.put(course, groups.get(g++)[0]);
                }
            }
            fits &= g < groups.size() && groups.get(g)[1] == 1;
            if (fits) {
                areas.put(area.getKey(), groups.get(g++)[0]);
            }
        }
        fits &= g == groups.size() - 1 && groups.get(g)[1] == 1;
        if (!fits) {
            throw new InvalidRequestException("El archivo no tiene las columnas de cursos de un examen de "
                    + type.getName() + ": revisa que sea el Excel de resultados de ese tipo de examen.");
        }
        return new Layout(courses, areas, groups.get(g)[0]);
    }

    private static ParsedRow toRow(int rowNumber, Row row, String name, Layout layout) {
        List<CourseCount> courses = new ArrayList<>();
        boolean attended = false;
        for (Map.Entry<Course, Integer> course : layout.courses().entrySet()) {
            int c = course.getValue();
            CourseCount count = new CourseCount(course.getKey().getId(), count(row, c), count(row, c + 1), count(row, c + 2));
            attended |= count.correct() > 0 || count.wrong() > 0;
            courses.add(count);
        }
        List<AreaScore> areas = new ArrayList<>();
        for (Map.Entry<Area, Integer> area : layout.areas().entrySet()) {
            BigDecimal score = score(row, area.getValue() + 2);
            if (score != null) {
                areas.add(new AreaScore(area.getKey().getId(), score));
            }
        }
        return new ParsedRow(rowNumber, dni(row.getCell(DNI_COLUMN)), name, courses, areas,
                score(row, layout.total() + 2), attended, text(cell(row, layout.total() + TUTOR_AFTER_TOTAL)));
    }

    /** A DNI or a name repeated among the students rejects the file: it is fixed in the spreadsheet. */
    private static void rejectRepeated(List<ParsedRow> rows) {
        List<String> errors = new ArrayList<>();
        Map<String, Integer> dniRows = new HashMap<>();
        Map<String, Integer> nameRows = new HashMap<>();
        for (ParsedRow row : rows) {
            if (row.dni() != null) {
                Integer first = dniRows.putIfAbsent(row.dni(), row.rowNumber());
                if (first != null) {
                    errors.add("fila " + row.rowNumber() + ": el DNI se repite en la fila " + first + ".");
                }
            }
            Integer first = nameRows.putIfAbsent(ImportNames.normalize(row.fullName()), row.rowNumber());
            if (first != null) {
                errors.add("fila " + row.rowNumber() + ": el nombre se repite en la fila " + first + ".");
            }
        }
        if (!errors.isEmpty()) {
            String shown = String.join(" · ", errors.subList(0, Math.min(errors.size(), MAX_ERRORS_SHOWN)));
            String more = errors.size() > MAX_ERRORS_SHOWN ? " · y " + (errors.size() - MAX_ERRORS_SHOWN) + " más." : "";
            throw new InvalidRequestException("Corrige el archivo y vuelve a subirlo: " + shown + more);
        }
    }

    /** The name, when the cell is text with letters; anything else (empty, a number) is not a student. */
    private static String studentName(Cell cell) {
        if (type(cell) != CellType.STRING) {
            return null;
        }
        String name = text(cell);
        return name != null && HAS_LETTER.matcher(name).find() ? name : null;
    }

    /**
     * "61180218.0", " 6118 0218 " → "61180218"; a number that lost its leading zero gets it back.
     * Only zeros ("0", the academy's placeholder while it lacks the DNI) is no DNI.
     */
    private static String dni(Cell cell) {
        String value = switch (type(cell)) {
            case NUMERIC -> {
                String digits = BigDecimal.valueOf(cell.getNumericCellValue()).toBigInteger().toString();
                yield digits.length() < 8 ? "0".repeat(8 - digits.length()) + digits : digits;
            }
            case STRING -> cell.getStringCellValue().replaceAll("\\s", "").replaceAll("\\.0+$", "");
            default -> "";
        };
        value = value.toUpperCase(java.util.Locale.ROOT);
        return VALID_DNI.matcher(value).matches() && !value.matches("0+") ? value : null;
    }

    private static int count(Row row, int column) {
        BigDecimal value = number(cell(row, column));
        return value == null ? 0 : value.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static BigDecimal score(Row row, int column) {
        BigDecimal value = number(cell(row, column));
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal number(Cell cell) {
        return switch (type(cell)) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                try {
                    yield new BigDecimal(cell.getStringCellValue().trim().replace(',', '.'));
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private static String token(Row row, int column) {
        String value = text(cell(row, column));
        return value == null ? null : ImportNames.normalize(value);
    }

    private static String text(Cell cell) {
        if (type(cell) != CellType.STRING) {
            return null;
        }
        String value = cell.getStringCellValue().trim().replaceAll("\\s+", " ");
        return value.isEmpty() ? null : value;
    }

    /** The cell's type, or its cached result's type for a formula. */
    private static CellType type(Cell cell) {
        if (cell == null) {
            return CellType.BLANK;
        }
        return cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
    }

    private static Cell cell(Row row, int column) {
        return row == null ? null : row.getCell(column);
    }

    private static boolean isBlank(Row row) {
        if (row == null) {
            return true;
        }
        for (Cell cell : row) {
            if (cell.getColumnIndex() < MERIT_COLUMN) {
                continue;
            }
            CellType type = type(cell);
            if (type == CellType.NUMERIC || type == CellType.BOOLEAN
                    || (type == CellType.STRING && !cell.getStringCellValue().isBlank())) {
                return false;
            }
        }
        return true;
    }
}
