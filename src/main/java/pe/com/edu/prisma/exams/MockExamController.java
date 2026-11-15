package pe.com.edu.prisma.exams;

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
import pe.com.edu.prisma.exams.dto.AddExamTypeRequest;
import pe.com.edu.prisma.exams.dto.CreateMockExamRequest;
import pe.com.edu.prisma.exams.dto.MockExamDto;
import pe.com.edu.prisma.exams.dto.UpdateMockExamRequest;

/**
 * Mock exams of a stage and their exam types (REQ-EXAMS-01): every role
 * reads them (a tutor works with their students' results); ADMIN and
 * COORDINADOR create, edit and delete them — the exam calendar is not a
 * tutor's day-to-day work.
 */
@RestController
@RequestMapping("/api/v1/mock-exams")
public class MockExamController {

    private final MockExamService mockExamService;

    public MockExamController(MockExamService mockExamService) {
        this.mockExamService = mockExamService;
    }

    @GetMapping
    public List<MockExamDto> findByStage(@RequestParam Long stageId) {
        return mockExamService.findByStage(stageId);
    }

    @GetMapping("/{id}")
    public MockExamDto findById(@PathVariable Long id) {
        return mockExamService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<MockExamDto> create(@Valid @RequestBody CreateMockExamRequest request) {
        MockExamDto created = mockExamService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/mock-exams/" + created.id())).body(created);
    }

    // name and date, for every exam type of the mock exam
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public MockExamDto update(@PathVariable Long id, @Valid @RequestBody UpdateMockExamRequest request) {
        return mockExamService.update(id, request);
    }

    // soft delete, only while none of its exams has results
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mockExamService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // answers the whole mock exam, so the client sees its types as they are now
    @PostMapping("/{id}/exams")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<MockExamDto> addExamType(@PathVariable Long id, @Valid @RequestBody AddExamTypeRequest request) {
        return ResponseEntity.status(201).body(mockExamService.addExamType(id, request));
    }

    @DeleteMapping("/{id}/exams/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public MockExamDto removeExam(@PathVariable Long id, @PathVariable Long examId) {
        return mockExamService.removeExam(id, examId);
    }
}
