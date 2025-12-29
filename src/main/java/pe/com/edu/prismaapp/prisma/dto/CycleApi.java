package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.Cycle;

import java.util.Date;

public interface CycleApi {

    @Schema(name = "CreateCycleRequest", description = "Datos requeridos para crear un ciclo")
    record Create(
            @NotBlank
            @Size(min = 4, max = 15)
            @Schema(description = "Nombre del ciclo", example = "2026-2")
            String name,

            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha inicio del ciclo", example = "01/01/2025")
            Date startDate,

            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha fin del ciclo", example = "01/03/2025")
            Date endDate
    ) {
    }

    @Schema(name = "UpdateCycleRequest", description = "Datos requeridos para actualizar un ciclo")
    record Update(
            @NotNull
            @Schema(description = "Id del ciclo", example = "1")
            Long id,

            @NotBlank
            @Size(min = 4, max = 15)
            @Schema(description = "Nombre del ciclo", example = "2026-2")
            String name,

            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha inicio del ciclo", example = "01/01/2025")
            Date startDate,

            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha fin del ciclo", example = "01/03/2025")
            Date endDate
    ) {}

    @Schema(name = "CycleResponse", description = "Respuesta con detalles del ciclo")
    record Response(
            @NotNull
            @Schema(description = "Id del ciclo", example = "1")
            Long id,

            @NotBlank
            @Size(min = 4, max = 15)
            @Schema(description = "Nombre del ciclo", example = "2026-2")
            String name,

            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha inicio del ciclo", example = "01/01/2025")
            Date startDate,

            @JsonFormat(pattern = "dd/MM/yyyy")
            @Schema(description = "Fecha fin del ciclo", example = "01/03/2025")
            Date endDate,

            @JsonProperty("isCurrent")
            @Schema(description = "Flag de ciclo actual", example = "true")
            boolean isCurrent
    ) {

        public static Response from(Cycle cycle) {
            return new Response(
                    cycle.getId(),
                    cycle.getName(),
                    cycle.getStartDate(),
                    cycle.getEndDate(),
                    cycle.isCurrent()
            );
        }
    }
}
