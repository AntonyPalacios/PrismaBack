package pe.com.edu.prisma.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertCourseRequest(

        @NotNull(message = "Selecciona a qué área pertenece el curso.")
        Long areaId,

        @NotBlank(message = "El curso necesita un nombre.")
        @Size(max = 100, message = "El nombre del curso es demasiado largo.")
        String name,

        @Size(max = 20, message = "La abreviatura es demasiado larga.")
        String abbreviation
) {
}
