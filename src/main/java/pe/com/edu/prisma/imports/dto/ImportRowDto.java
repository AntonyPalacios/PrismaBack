package pe.com.edu.prisma.imports.dto;

import java.util.List;
import pe.com.edu.prisma.imports.ImportRowStatus;

/**
 * A row of a students batch as the review shows it. {@code careerName} and
 * {@code tutorName} are what the Carrera and Tutor texts matched (null =
 * left without one, assigned by hand later). {@code student} is the
 * registered student it was matched to; {@code candidates} the ones it may be.
 */
public record ImportRowDto(
        Long id,
        int rowNumber,
        ImportRowStatus status,
        String fullName,
        String dni,
        String email,
        String phone,
        String birthDate,
        String careerText,
        String careerName,
        String tutorText,
        String tutorName,
        String notice,
        StudentRefDto student,
        List<StudentRefDto> candidates
) {
}
