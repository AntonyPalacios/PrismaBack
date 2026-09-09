package pe.com.edu.prisma.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertAreaRequest(

        @NotBlank(message = "El área necesita un nombre.")
        @Size(max = 100, message = "El nombre del área es demasiado largo.")
        String name,

        @Size(max = 20, message = "La abreviatura es demasiado larga.")
        String abbreviation
) {
}
