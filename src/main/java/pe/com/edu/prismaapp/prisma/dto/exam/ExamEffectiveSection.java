package pe.com.edu.prismaapp.prisma.dto.exam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "ExamEffectiveSectionResponse", description = "Respuesta con la cantidad de buenas y malas total en " +
        "lectura y matemática del examen de un alumno")
public record ExamEffectiveSection(
        @NotBlank
        @Schema(description = "Nombre del examen", example = "Regular 1")
        String name,

        @NotNull
        @Size(max = 28)
        @Schema(description = "Cantidad de correctas en Lectura", example = "10")
        long lectCorrect,

        @NotNull
        @Size(max = 28)
        @Schema(description = "Cantidad de incorrectas en Lectura", example = "3")
        long lectIncorrect,

        @NotNull
        @Size(max = 48)
        @Schema(description = "Cantidad de correctas en Matemática", example = "40")
        long mateCorrect,

        @NotNull
        @Size(max = 48)
        @Schema(description = "Cantidad de incorrectas en Matemática", example = "8")
        long mateIncorrect
) {
}
