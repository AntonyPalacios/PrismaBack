package pe.com.edu.prisma.tutoring;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.tutoring.dto.AssignTutorRequest;
import pe.com.edu.prisma.tutoring.dto.TutorAssignmentDto;

/**
 * Tutor of an enrollment (REQ-TUTORING-01): ADMIN and TUTOR read the
 * history (whoever sees enrollments); only ADMIN assigns. A coordinator
 * does not see enrollments, so neither their tutors.
 */
@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/tutor-assignments")
public class TutorAssignmentController {

    private final TutorAssignmentService assignmentService;

    public TutorAssignmentController(TutorAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
    public List<TutorAssignmentDto> findByEnrollment(@PathVariable Long enrollmentId) {
        return assignmentService.findByEnrollment(enrollmentId);
    }

    // 201 with the new assignment; 200 with the current one when it is the same tutor
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TutorAssignmentDto> assign(@PathVariable Long enrollmentId,
            @Valid @RequestBody AssignTutorRequest request) {
        TutorAssignmentService.Result result = assignmentService.assign(enrollmentId, request.tutorUserId());
        if (!result.created()) {
            return ResponseEntity.ok(result.assignment());
        }
        URI location = URI.create("/api/v1/enrollments/" + enrollmentId + "/tutor-assignments/" + result.assignment().id());
        return ResponseEntity.created(location).body(result.assignment());
    }
}
