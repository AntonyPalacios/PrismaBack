package pe.com.edu.prisma.students.dto;

/**
 * What the students list asks for: a name or DNI to search, whether to
 * include inactive students and, only for ADMIN, a tutor or "without tutor".
 */
public record StudentFilter(String search, boolean includeInactive, Long tutorId, boolean withoutTutor) {
}
