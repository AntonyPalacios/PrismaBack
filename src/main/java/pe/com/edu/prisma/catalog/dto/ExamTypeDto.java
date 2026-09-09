package pe.com.edu.prisma.catalog.dto;

import java.util.List;

public record ExamTypeDto(
        Long id,
        String name,
        boolean active,
        List<Long> courseIds,
        List<String> courseNames
) {
}
