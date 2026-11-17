package pe.com.edu.prisma.dashboards.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A student's balance across the Mathematics courses (REQ-DASHBOARDS-04,
 * agreements of 2026-11-17): their latest attended exam of the active cycle
 * and the one right before it, or null when there is none. Each axis is the
 * share of correct answers of one course evaluated in that exam, since the
 * number of questions per course changes in every exam.
 */
public record MathRadarDto(Exam current, Exam previous) {

    public record Exam(Long examId, String examName, String examTypeName, LocalDate examDate, List<Axis> axes) {
    }

    /** {@code percent} = correct / total × 100, two decimals; null when the course had no questions. */
    public record Axis(Long courseId, String courseName, int correct, int total, BigDecimal percent) {
    }
}
