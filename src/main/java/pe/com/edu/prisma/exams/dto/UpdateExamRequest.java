package pe.com.edu.prisma.exams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Name, date and type of one exam; the type only while it has no results. The stage is fixed. */
public record UpdateExamRequest(
        @NotBlank(message = "El examen necesita un nombre.")
        @Size(max = 200, message = "El nombre del examen es demasiado largo.")
        String name,

        @NotNull(message = "Ingresa la fecha del examen.")
        LocalDate examDate,

        @NotNull(message = "Elige el tipo de examen.")
        Long examTypeId
) {
}
