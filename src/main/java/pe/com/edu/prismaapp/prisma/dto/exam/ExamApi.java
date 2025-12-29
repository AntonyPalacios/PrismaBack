package pe.com.edu.prismaapp.prisma.dto.exam;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.Exam;

import java.util.Date;

public interface ExamApi {

    @Schema(name = "CreateExamRequest", description = "Datos requeridos para crear un examen")
    record Create(
            @NotBlank
            @Size(min = 3, max = 15)
            @Schema(description = "Nombre del examen", example = "Regular 1")
            String name,

            @NotNull
            @Schema(description = "Id de la etapa a la que pertenece el examen", example = "2")
            Long stageId,

            @NotNull
            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha del examen", example = "10/05/2025")
            Date date
    ) {
    }

    @Schema(name = "UpdateExamRequest", description = "Datos requeridos para actualizar un examen")
    record Update(
            @NotNull
            @Schema(description = "Id del examen", example = "3")
            Long id,

            @NotBlank
            @Size(min = 3, max = 15)
            @Schema(description = "Nombre del examen", example = "Regular 1")
            String name,

            @NotNull
            @Schema(description = "Id de la etapa a la que pertenece el examen", example = "2")
            Long stageId,

            @NotNull
            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha del examen", example = "10/05/2025")
            Date date
    ) {
    }

    @Schema(name = "ExamListResponse", description = "Respuesta con los detalles de los examenes")
    record ExamList(
            @NotNull
            @Schema(description = "Id del examen", example = "3")
            Long id,

            @NotBlank
            @Size(min = 3, max = 15)
            @Schema(description = "Nombre del examen", example = "Regular 1")
            String name,

            @NotNull
            @Schema(description = "Id de la etapa a la que pertenece el examen", example = "2")
            Long stageId,

            @NotNull
            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha del examen", example = "10/05/2025")
            Date date,

            @Schema(description = "Nombre del ciclo", example = "2025-1")
            String cycle,

            @Schema(description = "Nombre de la etapa", example = "Regular 2")
            String stage
    ) {
    }

    @Schema(name = "ExamResponse", description = "Respuesta con los detalles de los examenes")
    record Response(
            @NotNull
            @Schema(description = "Id del examen", example = "3")
            Long id,

            @NotBlank
            @Size(min = 3, max = 15)
            @Schema(description = "Nombre del examen", example = "Regular 1")
            String name,

            @NotNull
            @Schema(description = "Id de la etapa a la que pertenece el examen", example = "2")
            Long stageId,

            @NotNull
            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha del examen", example = "10/05/2025")
            Date date
    ) {
        public static Response from(Exam exam) {
            return new ExamApi.Response(
                    exam.getId(),
                    exam.getName(),
                    exam.getStage().getId(),
                    exam.getDate()
            );
        }
    }
}
