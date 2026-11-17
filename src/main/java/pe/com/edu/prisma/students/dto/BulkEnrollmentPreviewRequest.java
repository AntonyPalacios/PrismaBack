package pe.com.edu.prisma.students.dto;

import jakarta.validation.constraints.NotNull;

/** Mass enrollment (REQ-STUDENTS-05): whose students move, and where to. Only ids. */
public record BulkEnrollmentPreviewRequest(
        @NotNull(message = "Elige la etapa de origen.")
        Long sourceStageId,

        @NotNull(message = "Elige la etapa de destino.")
        Long targetStageId
) {
}
