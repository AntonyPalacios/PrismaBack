package pe.com.edu.prisma.academic.dto;

import jakarta.validation.constraints.NotNull;
import pe.com.edu.prisma.academic.CycleStatus;

/**
 * Move a cycle forward: to ACTIVE or to CLOSED. {@code confirm} is only
 * needed when the backend asked for it (closing before the end date).
 */
public record ChangeCycleStatusRequest(

        @NotNull(message = "Indica el nuevo estado del ciclo.")
        CycleStatus status,

        Boolean confirm
) {

    public boolean confirmed() {
        return Boolean.TRUE.equals(confirm);
    }
}
