package pe.com.edu.prismaapp.prisma.dto.exam;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Date;

@Schema(name = "ExamDataResponse", description = "Respuesta con los detalles de un exámen")
public record ExamData(
        @NotNull
        @Schema(description = "Id del examen", example = "3")
        Long id,

        @NotBlank
        @Size(min = 3, max = 15)
        @Schema(description = "Nombre del examen", example = "Regular 1")
        String name,

        @NotNull
        @JsonFormat(pattern = "dd/MM/yyyy")
        @Schema(description = "Fecha del examen", example = "10/05/2025")
        Date date
) {
}
