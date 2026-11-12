package pe.com.edu.prisma.academic.dto;

import jakarta.validation.constraints.NotNull;
import pe.com.edu.prisma.academic.StageStatus;

/**
 * Move a stage forward: to ACTIVE or to CLOSED. Unlike a cycle there is no
 * "confirm": the system never changes a stage's end date, so there is
 * nothing to accept — closing early means correcting the end date first.
 */
public record ChangeStageStatusRequest(

        @NotNull(message = "Indica el nuevo estado de la etapa.")
        StageStatus status
) {
}
