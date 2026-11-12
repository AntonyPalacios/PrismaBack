package pe.com.edu.prisma.academic.dto;

import java.time.LocalDate;
import pe.com.edu.prisma.academic.StageStatus;

/**
 * {@code current} marks the stage in force by date (the one containing
 * today; if none, the one that started most recently). It is unrelated to
 * {@code status}: a CLOSED stage can still be the current one.
 */
public record StageDto(
        Long id,
        Long cycleId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        StageStatus status,
        boolean current
) {
}
