package pe.com.edu.prisma.academic.dto;

import java.time.LocalDate;
import pe.com.edu.prisma.academic.CycleStatus;

public record CycleDto(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        CycleStatus status
) {
}
