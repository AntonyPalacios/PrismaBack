package pe.com.edu.prisma.exams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** A mock exam: one exam per type, same name, date and stage. Nothing here is personal data. */
public record CreateExamsRequest(
        @NotNull(message = "Elige la etapa del examen.")
        Long stageId,

        @NotBlank(message = "El examen necesita un nombre.")
        @Size(max = 200, message = "El nombre del examen es demasiado largo.")
        String name,

        @NotNull(message = "Ingresa la fecha del examen.")
        LocalDate examDate,

        @NotEmpty(message = "Elige al menos un tipo de examen.")
        List<Long> examTypeIds
) {
}
