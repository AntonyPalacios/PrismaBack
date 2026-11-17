package pe.com.edu.prisma.students.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Confirms a mass enrollment (REQ-STUDENTS-05): one row per student, career and tutor optional. Only ids. */
public record BulkEnrollmentRequest(
        @NotNull(message = "Elige la etapa de destino.")
        Long targetStageId,

        @NotEmpty(message = "Elige al menos un alumno para matricular.")
        List<@Valid @NotNull Row> rows
) {

    public record Row(
            @NotNull(message = "Indica el alumno que se matricula.")
            Long studentId,

            Long careerId,

            Long tutorUserId
    ) {
    }
}
