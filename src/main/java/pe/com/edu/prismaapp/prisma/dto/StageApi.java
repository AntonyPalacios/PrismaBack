package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.Stage;

import java.util.Date;

public interface StageApi {

    @Schema(name = "CreateStageRequest", description = "Datos requeridos para crear una etapa")
    record Create(
            @NotBlank
            @Size(min = 4, max = 15)
            @Schema(description = "Nombre de la etapa", example = "Regular 1")
            String name,

            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha inicio de la etapa", example = "01/01/2025")
            Date startDate,

            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha fin de la etapa", example = "01/03/2025")
            Date endDate,

            @NotNull
            @Schema(description = "Id del ciclo al que pertenece la etapa", example = "1")
            Long idCycle
    ){}

    @Schema(name = "UpdateStageRequest", description = "Datos requeridos para actualizar una etapa")
    record Update(
            @NotNull
            @Schema(description = "Id de la etapa", example = "2")
            Long id,

            @NotBlank
            @Size(min = 4, max = 15)
            @Schema(description = "Nombre de la etapa", example = "Regular 1")
            String name,

            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha inicio de la etapa", example = "01/01/2025")
            Date startDate,

            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha fin de la etapa", example = "01/03/2025")
            Date endDate,

            @NotNull
            @Schema(description = "Id del ciclo al que pertenece la etapa", example = "1")
            Long idCycle
    ){}

    @Schema(name = "StageResponse", description = "Respuesta con detalles de la etapa")
    record Response(
            @NotNull
            @Schema(description = "Id de la etapa", example = "2")
            Long id,

            @NotBlank
            @Size(min = 4, max = 15)
            @Schema(description = "Nombre de la etapa", example = "Regular 1")
            String name,

            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha inicio de la etapa", example = "01/01/2025")
            Date startDate,

            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha fin de la etapa", example = "01/03/2025")
            Date endDate,

            @NotNull
            @Schema(description = "Id del ciclo al que pertenece la etapa", example = "1")
            Long idCycle,

            @JsonProperty("isCurrent")
            @Schema(description = "Flag de etapa actual", example = "true")
            boolean isCurrent
    ){

        public static StageApi.Response from(Stage stage) {
            return new StageApi.Response(
                    stage.getId(),
                    stage.getName(),
                    stage.getStartDate(),
                    stage.getEndDate(),
                    stage.getCycle().getId(),
                    stage.isCurrent()
            );
        }
    }
}
