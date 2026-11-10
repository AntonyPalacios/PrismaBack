package pe.com.edu.prisma.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertCareerRequest(

        @NotBlank(message = "La carrera necesita un nombre.")
        @Size(max = 100, message = "El nombre de la carrera es demasiado largo.")
        String name,

        @Size(max = 20, message = "La abreviatura es demasiado larga.")
        String abbreviation,

        @NotNull(message = "Selecciona el tipo de examen de la carrera.")
        Long examTypeId
) {
}
