package pe.com.edu.prisma.tutoring.dto;

import java.time.OffsetDateTime;

/** One row of an enrollment's tutor history; {@code validTo} null = the current tutor. */
public record TutorAssignmentDto(
        Long id,
        Long enrollmentId,
        Long tutorId,
        String tutorName,
        OffsetDateTime validFrom,
        OffsetDateTime validTo
) {
}
