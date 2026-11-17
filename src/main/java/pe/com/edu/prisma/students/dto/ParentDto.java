package pe.com.edu.prisma.students.dto;

import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;

/** A guardian's contact data. */
public record ParentDto(
        Long id,
        @Sensitive(LogMask.NAME) String fullName,
        @Sensitive(LogMask.HIDDEN) String phone,
        @Sensitive(LogMask.EMAIL) String email
) {
}
