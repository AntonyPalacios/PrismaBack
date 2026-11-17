package pe.com.edu.prisma.exams.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * A student's result in an exam (REQ-EXAMS-04): correct, wrong and blank
 * answers per course, each area's score and the total, as they were loaded
 * or typed. {@code attended} is derived (0 correct and 0 wrong in every
 * course = did not attend); {@code updatedByName} is who touched it last.
 */
public record ExamResultDto(
        Long examId,
        Long enrollmentId,
        List<CourseResult> courseResults,
        List<AreaScore> areaScores,
        BigDecimal totalScore,
        boolean attended,
        String updatedByName,
        OffsetDateTime updatedAt
) {

    public record CourseResult(Long courseId, String courseName, int correct, int wrong, int blank) {
    }

    public record AreaScore(Long areaId, String areaName, BigDecimal score) {
    }
}
