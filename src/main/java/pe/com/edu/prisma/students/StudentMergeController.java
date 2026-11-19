package pe.com.edu.prisma.students;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.students.dto.MergePreviewDto;
import pe.com.edu.prisma.students.dto.MergeStudentsRequest;
import pe.com.edu.prisma.students.dto.StudentDto;

/** Merging duplicate students: ADMIN only, reading the preview included. */
@RestController
@RequestMapping("/api/v1/students/{keepId}")
@PreAuthorize("hasRole('ADMIN')")
public class StudentMergeController {

    private final StudentMergeService mergeService;

    public StudentMergeController(StudentMergeService mergeService) {
        this.mergeService = mergeService;
    }

    @GetMapping("/merge-preview")
    public MergePreviewDto preview(@PathVariable Long keepId, @RequestParam Long removeId) {
        return mergeService.preview(keepId, removeId);
    }

    @PostMapping("/merge")
    public StudentDto merge(@PathVariable Long keepId, @Valid @RequestBody MergeStudentsRequest request) {
        return mergeService.merge(keepId, request.removeId());
    }
}
