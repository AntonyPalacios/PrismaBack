package pe.com.edu.prisma.students;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.students.dto.CreateEnrollmentRequest;
import pe.com.edu.prisma.students.dto.EnrollmentDto;
import pe.com.edu.prisma.students.dto.UpdateEnrollmentRequest;

/**
 * Enrollments (REQ-STUDENTS-02): only ADMIN and TUTOR, reading included —
 * a coordinator does not see them. For now any tutor enrolls any student;
 * the tutoring slice narrows it to their own.
 */
@RestController
@RequestMapping("/api/v1/enrollments")
@PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public List<EnrollmentDto> findByStudent(@RequestParam Long studentId) {
        return enrollmentService.findByStudent(studentId);
    }

    @PostMapping
    public ResponseEntity<EnrollmentDto> create(@Valid @RequestBody CreateEnrollmentRequest request) {
        EnrollmentDto created = enrollmentService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/enrollments/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public EnrollmentDto update(@PathVariable Long id, @RequestBody UpdateEnrollmentRequest request) {
        return enrollmentService.update(id, request);
    }

    // withdraw from the stage (soft): the enrollment stays as history
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdraw(@PathVariable Long id) {
        enrollmentService.withdraw(id);
        return ResponseEntity.noContent().build();
    }
}
