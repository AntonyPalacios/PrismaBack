package pe.com.edu.prisma.imports.impl;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import pe.com.edu.prisma.imports.dto.StudentRefDto;
import pe.com.edu.prisma.students.Student;

/** How the imports compare and show people's names. */
final class ImportNames {

    private ImportNames() {
    }

    /** "Apellidos, Nombres", the agreed way to show a person. */
    static String displayName(String lastNamePat, String lastNameMat, String firstName) {
        return fullName(lastNamePat, lastNameMat) + ", " + firstName;
    }

    /** The non-blank parts joined by a space. */
    static String fullName(String... parts) {
        return Stream.of(parts).filter(Objects::nonNull).filter(p -> !p.isBlank()).collect(Collectors.joining(" "));
    }

    /** A student's "paterno materno nombres", normalized: how a single name cell of a spreadsheet is compared. */
    static String comparableName(Student student) {
        return normalize(fullName(student.getLastNamePat(), student.getLastNameMat(), student.getFirstName()));
    }

    /** Lower case, no accents, single spaces: "  Álvarez   PÉREZ " → "alvarez perez". */
    static String normalize(String text) {
        String noAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    static StudentRefDto ref(Student student) {
        return student == null ? null : new StudentRefDto(student.getId(),
                displayName(student.getLastNamePat(), student.getLastNameMat(), student.getFirstName()),
                student.getDni(), student.isActive());
    }
}
