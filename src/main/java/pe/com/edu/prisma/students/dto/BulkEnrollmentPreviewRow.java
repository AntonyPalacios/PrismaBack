package pe.com.edu.prisma.students.dto;

import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;

/**
 * A student enrolled in the source stage, with the career and tutor they
 * had there as editable starting points ({@code null} when that career is
 * inactive or that tutor is no longer an active TUTOR). {@code skipReason}
 * says why the batch will leave them out; {@code null} means they are enrolled.
 */
public record BulkEnrollmentPreviewRow(
        Long studentId,
        @Sensitive(LogMask.NAME) String fullName,
        Long careerId,
        Long tutorUserId,
        SkipReason skipReason
) {

    public enum SkipReason {
        /** Already enrolled in the target stage (withdrawn too: one enrollment per stage). */
        ALREADY_ENROLLED,
        /** The student was deactivated. */
        INACTIVE_STUDENT
    }
}
