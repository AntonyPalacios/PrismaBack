package pe.com.edu.prisma.logbook.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;
import pe.com.edu.prisma.logbook.MeetingWith;

/**
 * A new logbook entry: the session date, who the tutor met and three texts.
 * The texts are optional one by one (an empty reason or agreements asks for
 * {@code confirm: true}; observations are optional for good); they are about
 * a minor's counseling, so they are never logged, not even partially.
 */
public record CreateLogEntryRequest(
        @NotNull LocalDate sessionDate,
        @NotNull MeetingWith meetingWith,
        @Sensitive(LogMask.HIDDEN) @Size(max = 5000) String reason,
        @Sensitive(LogMask.HIDDEN) @Size(max = 5000) String agreements,
        @Sensitive(LogMask.HIDDEN) @Size(max = 5000) String observations,
        boolean confirm
) {
}
