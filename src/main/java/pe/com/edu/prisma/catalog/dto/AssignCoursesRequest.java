package pe.com.edu.prisma.catalog.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record AssignCoursesRequest(

        @NotEmpty(message = "Selecciona al menos un curso para este tipo de examen.")
        Set<Long> courseIds
) {
}
