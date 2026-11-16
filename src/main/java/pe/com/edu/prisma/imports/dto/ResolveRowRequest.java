package pe.com.edu.prisma.imports.dto;

import jakarta.validation.constraints.NotNull;
import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;

/**
 * How a person resolves a row: MATCH it to a registered student
 * ({@code studentId}), create it as a NEW student, or IGNORE it. A new
 * student from an exam results file needs its names split by hand (the file
 * has them in one cell): {@code firstName}, {@code lastNamePat},
 * {@code lastNameMat}; a students batch ignores them.
 */
public record ResolveRowRequest(
        @NotNull(message = "Indica cómo resolver la fila.")
        Action action,
        Long studentId,
        @Sensitive(LogMask.NAME)
        String firstName,
        @Sensitive(LogMask.NAME)
        String lastNamePat,
        @Sensitive(LogMask.NAME)
        String lastNameMat
) {

    public enum Action {
        MATCH,
        NEW,
        IGNORE
    }
}
