package pe.com.edu.prismaapp.prisma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.Area;

public interface AreaApi {

    @Schema(name = "AreaResponse", description = "Respuesta con la información de una área")
    record Response(
            @Schema(description = "ID del área", example = "1")
            @NotBlank
            Long id,

            @Schema(description = "Nombre del área", example = "Ciencias")
            @NotBlank
            @Size(min = 3, max = 15)
            String name,

            @Schema(description = "Abreviación del área", example = "CC")
            @NotBlank
            @Size(max = 15) String abbreviation,

            @Schema(description = "Indica si es área principal (CC,LL,Q)", example = "true")
            boolean main
    ) {

        public static AreaApi.Response from(Area area) {
            return new AreaApi.Response(
                    area.getId(),
                    area.getName(),
                    area.getAbbreviation(),
                    area.isMain()
            );
        }
    }
}
