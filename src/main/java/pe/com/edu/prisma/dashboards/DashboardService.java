package pe.com.edu.prisma.dashboards;

import java.util.List;
import pe.com.edu.prisma.dashboards.dto.AreaDistributionDto;
import pe.com.edu.prisma.dashboards.dto.CourseSeriesDto;
import pe.com.edu.prisma.dashboards.dto.GroupComparisonExamDto;
import pe.com.edu.prisma.dashboards.dto.ScoreEvolutionPointDto;

/** What the stored exam results say, ready to chart (REQ-DASHBOARDS). */
public interface DashboardService {

    /**
     * A student's total score exam after exam, by exam date: only the exams
     * they sat, of the active cycle unless {@code includeAllCycles}. An
     * admin reads anyone; a tutor only a student currently assigned to them.
     */
    List<ScoreEvolutionPointDto> scoreEvolution(Long studentId, boolean includeAllCycles);

    /** The same exams, as one series per course: correct, wrong and blank answers (REQ-DASHBOARDS-02). */
    List<CourseSeriesDto> courseEffectiveness(Long studentId, boolean includeAllCycles);

    /** The same exams, as correct and wrong answers per area and in total (REQ-DASHBOARDS-03). */
    AreaDistributionDto areaDistribution(Long studentId, boolean includeAllCycles);

    /**
     * "Charla de notas" (REQ-DASHBOARDS-05): how a group did in each exam of
     * that type in the active cycle, by exam date. The group of a tutor is
     * always their current students; an admin gets the whole academy, or
     * one tutor's group with {@code tutorId} (403 if a tutor sends it).
     */
    List<GroupComparisonExamDto> groupComparison(Long examTypeId, Long tutorId);
}
