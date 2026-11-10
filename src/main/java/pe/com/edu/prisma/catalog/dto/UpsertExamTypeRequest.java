package pe.com.edu.prisma.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * Create or edit an exam type in one request: its name and the full set of
 * courses it evaluates (replaces the previous set, all or nothing).
 */
public record UpsertExamTypeRequest(

        @NotBlank(message = "El tipo de examen necesita un nombre.")
        @Size(max = 50, message = "El nombre es demasiado largo.")
        String name,

        @NotEmpty(message = "Selecciona al menos un curso para este tipo de examen.")
        Set<Long> courseIds
) {
}
