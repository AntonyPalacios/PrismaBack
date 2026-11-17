package pe.com.edu.prisma.exams;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.exams.dto.ExamEnrollmentDto;
import pe.com.edu.prisma.exams.dto.ExamResultDto;
import pe.com.edu.prisma.exams.dto.SaveExamResultRequest;

/**
 * Creating or fixing one student's result by hand (REQ-EXAMS-04, agreements
 * of 2026-11-17): ADMIN and COORDINADOR for any student, a TUTOR only for
 * the students they tutor in that enrollment (checked in the service).
 */
@RestController
@RequestMapping("/api/v1/exams/{examId}")
@PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR', 'TUTOR')")
public class ExamResultController {

    private final ExamResultService examResultService;

    public ExamResultController(ExamResultService examResultService) {
        this.examResultService = examResultService;
    }

    @GetMapping("/enrollments")
    public List<ExamEnrollmentDto> findEnrollments(@PathVariable Long examId) {
        return examResultService.findEnrollments(examId);
    }

    // 404 while the student has no result: the screen opens an empty form
    @GetMapping("/results/{enrollmentId}")
    public ExamResultDto find(@PathVariable Long examId, @PathVariable Long enrollmentId) {
        return examResultService.find(examId, enrollmentId);
    }

    @PutMapping("/results/{enrollmentId}")
    public ExamResultDto save(@PathVariable Long examId, @PathVariable Long enrollmentId,
            @Valid @RequestBody SaveExamResultRequest request) {
        return examResultService.save(examId, enrollmentId, request);
    }
}
