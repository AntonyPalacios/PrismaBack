package pe.com.edu.prisma.goals.dto;

import java.time.LocalDate;
import java.util.List;
import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;

/**
 * A group's goals for one mock exam (REQ-GOALS-01): one row per student,
 * by name, with the exam they sit in it and, per area, the suggestion, the
 * goal and, once the exam has their result, the correct answers they got.
 */
public record GoalSheetDto(Long mockExamId, String mockExamName, LocalDate examDate, List<AreaDto> areas, List<Row> rows) {

    public record AreaDto(Long areaId, String areaName) {
    }

    /**
     * {@code examId} null = no goal can be set: {@code noExamReason} says why
     * (NO_EXAM_TYPE, or EXAM_TYPE_MISSING when the mock exam lacks their
     * {@code examTypeName}). {@code attended} is null while there is no result.
     */
    public record Row(
            Long enrollmentId,
            Long studentId,
            @Sensitive(LogMask.NAME) String fullName,
            String tutorName,
            Long examId,
            String examTypeName,
            String noExamReason,
            Boolean attended,
            List<AreaGoal> goals
    ) {
    }

    /**
     * {@code suggested} = correct answers in {@code basedOnExamName} (the
     * last attended exam before this one) + 2, null without one;
     * {@code target} null = no goal yet; {@code actual} only once they sat it.
     */
    public record AreaGoal(Long areaId, Integer suggested, String basedOnExamName, Integer target, Integer actual) {
    }
}
