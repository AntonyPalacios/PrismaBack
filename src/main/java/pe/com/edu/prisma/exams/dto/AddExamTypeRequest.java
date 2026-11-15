package pe.com.edu.prisma.exams.dto;

import jakarta.validation.constraints.NotNull;

/** An exam type the mock exam was missing. */
public record AddExamTypeRequest(
        @NotNull(message = "Elige el tipo de examen.")
        Long examTypeId
) {
}
