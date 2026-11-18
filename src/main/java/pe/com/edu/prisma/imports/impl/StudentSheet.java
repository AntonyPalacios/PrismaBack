package pe.com.edu.prisma.imports.impl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import pe.com.edu.prisma.common.exception.InvalidRequestException;
import pe.com.edu.prisma.students.dto.UpsertStudentRequest;

/**
 * The students template (REQ-IMPORTS-02): writes it and reads it back.
 * Columns are read by position — the header texts are never checked — in
 * this order: Nombres, Apellido paterno, Apellido materno, DNI, Correo,
 * Teléfono, Fecha de nacimiento, Carrera, Tutor. Each row is validated with
 * the same rules (and messages) as registering a student by hand; any
 * invalid row rejects the whole file, so it is fixed in the spreadsheet.
 * Error messages only cite row numbers: never the data itself.
 */
@Component
public class StudentSheet {

    static final List<String> HEADERS = List.of("Nombres *", "Apellido paterno *", "Apellido materno", "DNI",
            "Correo", "Teléfono", "Fecha de nacimiento", "Carrera", "Tutor");
    private static final int COLUMNS = HEADERS.size();
    private static final int MAX_ERRORS_SHOWN = 10;
    // strict: 31/02/2009 is an error, never "the last day of February"
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT));

    /** A valid row: the student as the hand form would send it, plus the career and tutor texts. */
    public record ParsedRow(int rowNumber, UpsertStudentRequest student, String careerText, String tutorText) {
    }

    private final Validator validator;
    private final DataFormatter formatter = new DataFormatter();

    public StudentSheet(Validator validator) {
        this.validator = validator;
    }

    /** The empty template with its headers, ready to fill. */
    public byte[] template() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Alumnos");
            Font bold = workbook.createFont();
            bold.setBold(true);
            CellStyle header = workbook.createCellStyle();
            header.setFont(bold);
            Row row = sheet.createRow(0);
            for (int i = 0; i < COLUMNS; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(HEADERS.get(i));
                cell.setCellStyle(header);
                sheet.setColumnWidth(i, 22 * 256);
            }
            // the DNI as text, so leading zeros survive
            CellStyle text = workbook.createCellStyle();
            text.setDataFormat(workbook.createDataFormat().getFormat("@"));
            sheet.setDefaultColumnStyle(3, text);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not build the students template", e);
        }
    }

    /** The file's rows, or an {@link InvalidRequestException} listing every problem. */
    public List<ParsedRow> read(InputStream file) {
        Sheet sheet = openFirstSheet(file);
        checkColumns(sheet);

        List<ParsedRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Map<String, Integer> dniRows = new HashMap<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isBlank(row)) {
                continue; // trailing or spacer rows
            }
            int rowNumber = i + 1; // as the user sees it in the spreadsheet
            List<String> rowErrors = new ArrayList<>();
            LocalDate birthDate = date(row.getCell(6), rowErrors);
            UpsertStudentRequest student = new UpsertStudentRequest(text(row, 0), text(row, 1), text(row, 2),
                    dni(row.getCell(3)), text(row, 4), text(row, 5), birthDate);
            validator.validate(student).stream()
                    .sorted(Comparator.comparing(v -> fieldOrder(v.getPropertyPath().toString())))
                    .map(ConstraintViolation::getMessage)
                    .forEach(rowErrors::add);
            if (student.dni() != null) {
                Integer first = dniRows.putIfAbsent(student.dni(), rowNumber);
                if (first != null) {
                    rowErrors.add("El DNI se repite en la fila " + first + ".");
                }
            }
            rowErrors.forEach(error -> errors.add("fila " + rowNumber + ": " + error));
            rows.add(new ParsedRow(rowNumber, student, text(row, 7), text(row, 8)));
        }

        if (!errors.isEmpty()) {
            String shown = String.join(" · ", errors.subList(0, Math.min(errors.size(), MAX_ERRORS_SHOWN)));
            String more = errors.size() > MAX_ERRORS_SHOWN ? " · y " + (errors.size() - MAX_ERRORS_SHOWN) + " más." : "";
            throw new InvalidRequestException("Corrige el archivo y vuelve a subirlo: " + shown + more);
        }
        if (rows.isEmpty()) {
            throw new InvalidRequestException("El archivo no tiene alumnos: llena la plantilla desde la segunda fila.");
        }
        return rows;
    }

    private static Sheet openFirstSheet(InputStream file) {
        try {
            Workbook workbook = WorkbookFactory.create(file);
            return workbook.getSheetAt(0);
        } catch (IOException | RuntimeException e) {
            throw new InvalidRequestException("No pudimos leer el archivo: sube la plantilla de alumnos en formato Excel (.xlsx).");
        }
    }

    /** Exactly the template's columns: the first row has its 9 headers and nothing beyond them. */
    private void checkColumns(Sheet sheet) {
        Row header = sheet.getRow(0);
        boolean headersFilled = header != null;
        for (int i = 0; headersFilled && i < COLUMNS; i++) {
            headersFilled = !formatter.formatCellValue(header.getCell(i)).isBlank();
        }
        boolean extra = false;
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getColumnIndex() >= COLUMNS && !formatter.formatCellValue(cell).isBlank()) {
                    extra = true;
                }
            }
        }
        if (!headersFilled || extra) {
            throw new InvalidRequestException("El archivo no tiene las columnas de la plantilla de alumnos: "
                    + "descarga la plantilla y copia los datos en sus " + COLUMNS + " columnas, en el mismo orden.");
        }
    }

    private boolean isBlank(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < COLUMNS; i++) {
            if (!formatter.formatCellValue(row.getCell(i)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String text(Row row, int column) {
        String value = formatter.formatCellValue(row.getCell(column)).trim().replaceAll("\\s+", " ");
        return value.isEmpty() ? null : value;
    }

    /**
     * "61180218.0", " 6118 0218 " → "61180218"; letters stay (a foreigner's card).
     * Only zeros ("0", a placeholder while the DNI is unknown) is no DNI.
     */
    private String dni(Cell cell) {
        String value = formatter.formatCellValue(cell).replaceAll("\\s", "").replaceAll("\\.0+$", "");
        return value.isEmpty() || value.matches("0+") ? null : value;
    }

    /** A real date cell, or text such as 17/05/2009 or 2009-05-17. */
    private LocalDate date(Cell cell, List<String> errors) {
        if (cell == null || formatter.formatCellValue(cell).isBlank()) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = formatter.formatCellValue(cell).trim();
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, format);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        errors.add("La fecha de nacimiento no tiene un formato válido (usa dd/mm/aaaa).");
        return null;
    }

    // a row's errors in the order of its columns
    private static int fieldOrder(String field) {
        List<String> order = List.of("firstName", "lastNamePat", "lastNameMat", "dni", "email", "phone", "birthDate");
        int index = order.indexOf(field);
        return index < 0 ? order.size() : index;
    }
}
