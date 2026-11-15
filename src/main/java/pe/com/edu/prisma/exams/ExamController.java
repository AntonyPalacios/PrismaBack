package pe.com.edu.prisma.exams;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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
import pe.com.edu.prisma.exams.dto.CreateExamsRequest;
import pe.com.edu.prisma.exams.dto.ExamDto;
import pe.com.edu.prisma.exams.dto.UpdateExamRequest;

/**
 * Exams of a stage (REQ-EXAMS-01): every role reads them (a tutor works
 * with their students' results); ADMIN and COORDINADOR create, edit and
 * delete them — the exam calendar is not a tutor's day-to-day work.
 */
@RestController
@RequestMapping("/api/v1/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public List<ExamDto> findByStage(@RequestParam Long stageId) {
        return examService.findByStage(stageId);
    }

    @GetMapping("/{id}")
    public ExamDto findById(@PathVariable Long id) {
        return examService.findById(id);
    }

    // one exam per type, so the answer is a list (no single Location to point to)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<List<ExamDto>> create(@Valid @RequestBody CreateExamsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ExamDto update(@PathVariable Long id, @Valid @RequestBody UpdateExamRequest request) {
        return examService.update(id, request);
    }

    // soft delete, only while the exam has no results
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
