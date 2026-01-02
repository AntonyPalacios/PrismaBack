package pe.com.edu.prismaapp.prisma.dto.exam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "ExamEffectiveCourseResponse", description = "Respuesta con la cantidad de buenas y malas total " +
        "por curso del examen de un alumno")
public record ExamEffectiveCourse(
        @NotBlank
        @Schema(description = "Nombre del examen", example = "Regular 1")
        String name,

        @NotNull
        @Schema(description = "Cantidad de correctas en Lectura", example = "10")
        long lectCorrect,

        @NotNull
        @Schema(description = "Cantidad de incorrectas en Lectura", example = "2")
        long lectIncorrect,

        @NotNull
        @Schema(description = "Cantidad de correctas en NYO", example = "10")
        long nyoCorrect,

        @NotNull
        @Schema(description = "Cantidad de incorrectas en NYO", example = "2")
        long nyoIncorrect,

        @NotNull
        @Schema(description = "Cantidad de correctas en Álgebra", example = "10")
        long xCorrect,

        @NotNull
        @Schema(description = "Cantidad de incorrectas en Álgebra", example = "2")
        long xIncorrect,

        @NotNull
        @Schema(description = "Cantidad de correctas en Geometría", example = "10")
        long geoCorrect,

        @NotNull
        @Schema(description = "Cantidad de incorrectas en Geometría", example = "2")
        long geoIncorrect,

        @NotNull
        @Schema(description = "Cantidad de correctas en Trigonometría", example = "10")
        long trigoCorrect,

        @NotNull
        @Schema(description = "Cantidad de incorrectas en Trigonometría", example = "2")
        long trigoIncorrect,

        @NotNull
        @Schema(description = "Cantidad de correctas en Estadística", example = "10")
        long estCorrect,

        @NotNull
        @Schema(description = "Cantidad de incorrectas en Estadística", example = "2")
        long estIncorrect
) {
}
