package pe.com.edu.prismaapp.prisma.dto.exam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "ExamScoreResponse", description = "Respuesta con el resultados del examen de un alumno")
public record ExamScore(
        @NotBlank
        @Schema(description = "Nombre del examen", example = "Regular 1")
        String name,

        @NotNull
        @Schema(description = "Puntaje del alumno en el examen", example = "753.24")
        double score,

        @NotNull
        @Schema(description = "Mérito del alumno en el examen", example = "4")
        int merit,

        @NotNull
        @Schema(description = "Puntaje mínimo del examen", example = "421.5")
        double min,

        @NotNull
        @Schema(description = "Puntaje máximo del examen", example = "801.78")
        double max,

        @NotNull
        @Schema(description = "Puntaje promedio del examen", example = "711.5")
        double avg
) {
}
