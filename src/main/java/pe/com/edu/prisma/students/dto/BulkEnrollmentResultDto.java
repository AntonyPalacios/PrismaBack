package pe.com.edu.prisma.students.dto;

/** How a mass enrollment went: enrolled students and the ones left out (already enrolled or deactivated). */
public record BulkEnrollmentResultDto(int created, int skipped) {
}
