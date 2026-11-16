package pe.com.edu.prisma.imports.dto;

/** A registered student a row may be: "Apellidos, Nombres", DNI and whether they are active. */
public record StudentRefDto(Long id, String fullName, String dni, boolean active) {
}
