package pe.com.edu.prisma.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertExamTypeRequest(

        @NotBlank(message = "El tipo de examen necesita un nombre.")
        @Size(max = 50, message = "El nombre es demasiado largo.")
        String name
) {
}
