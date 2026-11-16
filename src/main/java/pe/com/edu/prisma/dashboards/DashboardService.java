package pe.com.edu.prisma.dashboards;

import java.util.List;
import pe.com.edu.prisma.dashboards.dto.ScoreEvolutionPointDto;

/** What the stored exam results say, ready to chart (REQ-DASHBOARDS). */
public interface DashboardService {

    /**
     * A student's total score exam after exam, by exam date: only the exams
     * they sat, of the active cycle unless {@code includeAllCycles}. An
     * admin reads anyone; a tutor only a student currently assigned to them.
     */
    List<ScoreEvolutionPointDto> scoreEvolution(Long studentId, boolean includeAllCycles);
}
