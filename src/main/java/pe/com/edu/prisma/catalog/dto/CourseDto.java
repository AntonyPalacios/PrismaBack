package pe.com.edu.prisma.catalog.dto;

public record CourseDto(
        Long id,
        Long areaId,
        String areaName,
        String name,
        String abbreviation,
        boolean active,
        // derived, not a straight column copy — shows the pattern for any
        // future field the frontend wants that isn't 1:1 with a DB column
        String displayLabel
) {
}
