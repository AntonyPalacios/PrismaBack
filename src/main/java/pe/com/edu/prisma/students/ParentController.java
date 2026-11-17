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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.students.dto.LinkParentRequest;
import pe.com.edu.prisma.students.dto.ParentDto;
import pe.com.edu.prisma.students.dto.StudentParentDto;
import pe.com.edu.prisma.students.dto.UpsertParentRequest;

/**
 * Guardians (REQ-STUDENTS-03): ADMIN and TUTOR only, reading included — a
 * coordinator does not see families' contact data. The service narrows
 * writes to the admin or the student's current tutor.
 */
@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
public class ParentController {

    private final ParentService parentService;

    public ParentController(ParentService parentService) {
        this.parentService = parentService;
    }

    @GetMapping("/api/v1/parents")
    public List<ParentDto> search(@RequestParam(defaultValue = "") String search) {
        return parentService.search(search);
    }

    @PostMapping("/api/v1/parents")
    public ResponseEntity<ParentDto> create(@Valid @RequestBody UpsertParentRequest request) {
        ParentDto created = parentService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/parents/" + created.id())).body(created);
    }

    @PutMapping("/api/v1/parents/{id}")
    public ParentDto update(@PathVariable Long id, @Valid @RequestBody UpsertParentRequest request) {
        return parentService.update(id, request);
    }

    @GetMapping("/api/v1/students/{studentId}/parents")
    public List<StudentParentDto> findByStudent(@PathVariable Long studentId) {
        return parentService.findByStudent(studentId);
    }

    @PostMapping("/api/v1/students/{studentId}/parents")
    public ResponseEntity<StudentParentDto> link(@PathVariable Long studentId, @Valid @RequestBody LinkParentRequest request) {
        StudentParentDto linked = parentService.link(studentId, request);
        return ResponseEntity.created(URI.create("/api/v1/students/" + studentId + "/parents/" + linked.parentId()))
                .body(linked);
    }

    @DeleteMapping("/api/v1/students/{studentId}/parents/{parentId}")
    public ResponseEntity<Void> unlink(@PathVariable Long studentId, @PathVariable Long parentId) {
        parentService.unlink(studentId, parentId);
        return ResponseEntity.noContent().build();
    }
}
