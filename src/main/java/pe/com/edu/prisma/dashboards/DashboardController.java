package pe.com.edu.prisma.dashboards;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.dashboards.dto.ScoreEvolutionPointDto;

/**
 * Dashboards over the exam results (REQ-DASHBOARDS). ADMIN and TUTOR; a
 * tutor only for their current students (checked by the service).
 */
@RestController
@RequestMapping("/api/v1/dashboards")
@PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/students/{studentId}/score-evolution")
    public List<ScoreEvolutionPointDto> scoreEvolution(@PathVariable Long studentId,
            @RequestParam(defaultValue = "false") boolean includeAllCycles) {
        return dashboardService.scoreEvolution(studentId, includeAllCycles);
    }
}
