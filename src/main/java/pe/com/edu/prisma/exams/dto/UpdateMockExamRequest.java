package pe.com.edu.prisma.exams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Name and date of a mock exam: they apply to all its exam types. The stage is fixed. */
public record UpdateMockExamRequest(
        @NotBlank(message = "El examen necesita un nombre.")
        @Size(max = 200, message = "El nombre del examen es demasiado largo.")
        String name,

        @NotNull(message = "Ingresa la fecha del examen.")
        LocalDate examDate
) {
}
