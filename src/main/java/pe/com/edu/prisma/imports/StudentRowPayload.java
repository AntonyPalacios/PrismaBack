package pe.com.edu.prisma.imports;

/**
 * A row of the students template as it was read. {@code careerId} and
 * {@code tutorId} are what the Carrera and Tutor texts matched when the file
 * was uploaded (null = no single match: assigned by hand later).
 * {@code notice} explains why a row that matched a student still needs a
 * person. Everything here is personal data: it never goes to the logs.
 */
public record StudentRowPayload(
        String firstName,
        String lastNamePat,
        String lastNameMat,
        String dni,
        String email,
        String phone,
        String birthDate,
        String careerText,
        Long careerId,
        String tutorText,
        Long tutorId,
        String notice
) {
}
