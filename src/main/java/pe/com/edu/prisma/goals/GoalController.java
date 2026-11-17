package pe.com.edu.prisma.goals;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.goals.dto.GoalMockExamDto;
import pe.com.edu.prisma.goals.dto.GoalSheetDto;
import pe.com.edu.prisma.goals.dto.SaveGoalSheetRequest;
import pe.com.edu.prisma.goals.dto.StudentGoalDto;

/** Goals (REQ-GOALS-01): ADMIN and TUTOR only; a coordinator takes no part. */
@RestController
@RequestMapping("/api/v1/goals")
@PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public List<StudentGoalDto> findByStudent(@RequestParam Long studentId) {
        return goalService.findByStudent(studentId);
    }

    @GetMapping("/mock-exams")
    public List<GoalMockExamDto> mockExams() {
        return goalService.mockExams();
    }

    @GetMapping("/sheet")
    public GoalSheetDto sheet(@RequestParam Long mockExamId, @RequestParam(required = false) Long tutorId) {
        return goalService.sheet(mockExamId, tutorId);
    }

    @PutMapping("/sheet")
    public ResponseEntity<Void> save(@Valid @RequestBody SaveGoalSheetRequest request) {
        goalService.save(request);
        return ResponseEntity.noContent().build();
    }
}
