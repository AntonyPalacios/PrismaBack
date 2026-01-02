package pe.com.edu.prismaapp.prisma.dto.exam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "ExamSectionSummaryResponse", description = "Respuesta con los agregados de efectivas por área (CC, LL, Q, TODOS)")
public record ExamSectionSummary(
        @NotNull
        @Schema(description = "Id del examen", example = "3")
        long examId,

        @NotNull
        @Schema(description = "Cantidad de correctas mínima por área del examen", example = "10")
        int minCorrect,

        @NotNull
        @Schema(description = "Cantidad de incorrectas mínima por área del examen", example = "3")
        int minIncorrect,

        @NotNull
        @Schema(description = "Cantidad de correctas máxima por área del examen", example = "20")
        int maxCorrect,

        @NotNull
        @Schema(description = "Cantidad de incorrectas máxima por área del examen", example = "5")
        int maxIncorrect,

        @NotNull
        @Schema(description = "Cantidad de correctas promedio por área del examen", example = "6")
        double avgCorrect,

        @NotNull
        @Schema(description = "Cantidad de incorrectas promedio por área del examen", example = "3")
        double avgIncorrect
) {
}
