package pe.com.edu.prisma.students.dto;

import java.time.LocalDate;

/**
 * A student's file plus where they stand today: the stage and tutor of their
 * current enrollment (null when they have none, or no tutor yet).
 */
public record StudentDto(
        Long id,
        String firstName,
        String lastNamePat,
        String lastNameMat,
        String dni,
        String email,
        String phone,
        LocalDate birthDate,
        boolean active,
        String currentStageName,
        Long tutorId,
        String tutorName,
        // the career of the current enrollment and its exam type (null without one)
        String careerName,
        Long examTypeId,
        String examTypeName
) {
}
