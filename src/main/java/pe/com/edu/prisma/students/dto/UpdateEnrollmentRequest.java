package pe.com.edu.prisma.students.dto;

/** Only the career changes; null clears it. Student and stage are fixed once enrolled. */
public record UpdateEnrollmentRequest(Long careerId) {
}
