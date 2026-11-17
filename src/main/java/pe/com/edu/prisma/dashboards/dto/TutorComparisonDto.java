package pe.com.edu.prisma.dashboards.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * How each tutor's group did in the last 3 exams of a type in the active
 * cycle (REQ-DASHBOARDS-06, agreements of 2026-11-17), against the whole
 * academy. {@code exams} by exam date; a tutor's {@code cells} only for the
 * exams their group sat (none = no data, never 0). Per area, the average
 * correct and wrong answers of the students who attended.
 */
public record TutorComparisonDto(List<Exam> exams, List<TutorRow> tutors, List<Cell> academy) {

    public record Exam(Long examId, String examName, LocalDate examDate) {
    }

    public record TutorRow(Long tutorId, String tutorName, List<Cell> cells) {
    }

    public record Cell(Long examId, int students, List<AreaAverage> areas) {
    }

    public record AreaAverage(Long areaId, String areaName, BigDecimal correctAvg, BigDecimal wrongAvg) {
    }
}
