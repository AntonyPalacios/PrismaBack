package pe.com.edu.prisma.students.dto;

import jakarta.validation.constraints.NotNull;

/** Only ids: nothing here is personal data. The career is optional. */
public record CreateEnrollmentRequest(
        @NotNull(message = "Indica el alumno que se matricula.")
        Long studentId,

        @NotNull(message = "Elige la etapa de la matrícula.")
        Long stageId,

        Long careerId
) {
}
