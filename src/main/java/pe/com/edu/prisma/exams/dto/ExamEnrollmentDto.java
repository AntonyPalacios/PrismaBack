package pe.com.edu.prisma.exams.dto;

import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;

/** A student enrolled in an exam's stage, to pick whose result to type; {@code hasResult} = it is already there. */
public record ExamEnrollmentDto(
        Long enrollmentId,
        Long studentId,
        @Sensitive(LogMask.NAME) String fullName,
        @Sensitive(LogMask.DNI) String dni,
        boolean hasResult
) {
}
