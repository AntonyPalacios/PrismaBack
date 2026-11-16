package pe.com.edu.prisma.imports.dto;

import jakarta.validation.constraints.NotNull;

/**
 * How a person resolves a row: MATCH it to a registered student
 * ({@code studentId}), create it as a NEW student, or IGNORE it.
 */
public record ResolveRowRequest(
        @NotNull(message = "Indica cómo resolver la fila.")
        Action action,

        Long studentId
) {

    public enum Action {
        MATCH,
        NEW,
        IGNORE
    }
}
