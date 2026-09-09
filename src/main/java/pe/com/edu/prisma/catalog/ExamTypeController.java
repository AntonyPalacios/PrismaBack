package pe.com.edu.prisma.catalog;

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
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.catalog.dto.AssignCoursesRequest;
import pe.com.edu.prisma.catalog.dto.ExamTypeDto;
import pe.com.edu.prisma.catalog.dto.UpsertExamTypeRequest;

@RestController
@RequestMapping("/api/v1/exam-types")
public class ExamTypeController {

    private final ExamTypeService examTypeService;

    public ExamTypeController(ExamTypeService examTypeService) {
        this.examTypeService = examTypeService;
    }

    @GetMapping
    public List<ExamTypeDto> findAll() {
        return examTypeService.findAll();
    }

    @GetMapping("/{id}")
    public ExamTypeDto findById(@PathVariable Long id) {
        return examTypeService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExamTypeDto> create(@Valid @RequestBody UpsertExamTypeRequest request) {
        ExamTypeDto created = examTypeService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/exam-types/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ExamTypeDto update(@PathVariable Long id, @Valid @RequestBody UpsertExamTypeRequest request) {
        return examTypeService.update(id, request);
    }

    // which courses this career track evaluates (RF-03.02) — a full
    // replace, not an incremental add/remove, keeps the semantics simple.
    @PutMapping("/{id}/courses")
    @PreAuthorize("hasRole('ADMIN')")
    public ExamTypeDto assignCourses(@PathVariable Long id, @Valid @RequestBody AssignCoursesRequest request) {
        return examTypeService.assignCourses(id, request.courseIds());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        examTypeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
