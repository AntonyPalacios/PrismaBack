package pe.com.edu.prisma.dashboards.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * How a group of students did in one exam ("charla de notas",
 * REQ-DASHBOARDS-05). {@code students}: how many of the group sat it. Per
 * area, the average, lowest and highest count of correct and of wrong
 * answers (each student's sum of the area's courses); per course the exam
 * evaluated, the average of correct answers. A zero is a value like any
 * other; only students who did not attend are left out.
 */
public record GroupComparisonExamDto(
        Long examId,
        String examName,
        LocalDate examDate,
        int students,
        List<AreaStats> areas,
        List<CourseAverage> courses
) {

    public record AreaStats(Long areaId, String areaName, BigDecimal correctAvg, int correctMin, int correctMax,
            BigDecimal wrongAvg, int wrongMin, int wrongMax) {
    }

    public record CourseAverage(Long courseId, String courseName, BigDecimal correctAvg) {
    }
}
