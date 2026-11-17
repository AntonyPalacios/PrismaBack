package pe.com.edu.prisma.logbook.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import pe.com.edu.prisma.logbook.MeetingWith;

/** A logbook entry as written; {@code authorName} is "Apellidos, Nombres", also for former tutors. */
public record LogEntryDto(
        Long id,
        Long studentId,
        Long authorUserId,
        String authorName,
        LocalDate sessionDate,
        MeetingWith meetingWith,
        OffsetDateTime createdAt,
        String reason,
        String agreements,
        String observations
) {
}
