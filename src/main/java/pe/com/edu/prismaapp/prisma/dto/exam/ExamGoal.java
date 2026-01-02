package pe.com.edu.prismaapp.prisma.dto.exam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "ExamGoalResponse", description = "Respuesta con las metas por exámenes de un alumno")
public record ExamGoal(
        @NotBlank
        @Size(min = 3, max = 15)
        @Schema(description = "Nombre del examen", example = "Regular 1")
        String name,

        @NotNull
        @Schema(description = "Puntaje del alumno en el examen", example = "753.24")
        double score,

        @NotNull
        @Schema(description = "Meta del alumno en el examen", example = "720.24")
        double goal,

        @NotNull
        @Schema(description = "Mérito del alumno en el examen", example = "4")
        int merit
) {
}
