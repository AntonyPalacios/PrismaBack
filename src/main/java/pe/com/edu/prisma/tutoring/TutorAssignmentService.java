package pe.com.edu.prisma.tutoring;

import java.util.List;
import pe.com.edu.prisma.tutoring.dto.TutorAssignmentDto;

/** Tutor of an enrollment, with history (REQ-TUTORING-01). */
public interface TutorAssignmentService {

    /** The enrollment's tutor history, newest first. */
    List<TutorAssignmentDto> findByEnrollment(Long enrollmentId);

    /**
     * Closes the current assignment (if any) and opens one for the tutor.
     * Choosing the current tutor again changes nothing: {@code created} is false.
     */
    Result assign(Long enrollmentId, Long tutorUserId);

    record Result(TutorAssignmentDto assignment, boolean created) {
    }
}
