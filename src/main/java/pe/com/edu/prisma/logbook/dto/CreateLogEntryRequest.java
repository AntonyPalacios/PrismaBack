package pe.com.edu.prisma.logbook.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;

/**
 * A new logbook entry. The three texts are optional one by one (an empty one
 * asks for {@code confirm: true}); they are about a minor's counseling, so
 * they are never logged, not even partially.
 */
public record CreateLogEntryRequest(
        @NotNull LocalDate sessionDate,
        @Sensitive(LogMask.HIDDEN) @Size(max = 5000) String reason,
        @Sensitive(LogMask.HIDDEN) @Size(max = 5000) String agreements,
        @Sensitive(LogMask.HIDDEN) @Size(max = 5000) String observations,
        boolean confirm
) {
}
