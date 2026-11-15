package pe.com.edu.prisma.students.dto;

/**
 * The enrollment's editable data, saved together: the career (null clears
 * it) and, only for ADMIN, the tutor (null = leave the tutor as it is).
 * Student and stage are fixed once enrolled.
 */
public record UpdateEnrollmentRequest(Long careerId, Long tutorUserId) {
}
