package pe.com.edu.prisma.catalog.dto;

public record CareerDto(
        Long id,
        String name,
        String abbreviation,
        boolean active,
        Long examTypeId,
        String examTypeName
) {
}
