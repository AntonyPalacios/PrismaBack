package pe.com.edu.prisma.students.dto;

import jakarta.validation.constraints.NotNull;

/** The duplicate that disappears into the student of the path. Only an id. */
public record MergeStudentsRequest(
        @NotNull(message = "Elige el alumno que se fusiona.")
        Long removeId
) {
}
