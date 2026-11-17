package pe.com.edu.prisma.exams.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;

/**
 * Creates or overwrites a student's result (REQ-EXAMS-04): one entry per
 * course of the exam's type and one per area. Scores are typed as the
 * academy's file brings them; the system never calculates them. Scores are
 * never logged (agreements, section 4.2): the lists only by their size and
 * the total hidden.
 */
public record SaveExamResultRequest(
        @NotEmpty @Valid List<CourseResult> courseResults,
        @NotEmpty @Valid List<AreaScore> areaScores,
        @Sensitive(LogMask.HIDDEN) @NotNull @PositiveOrZero @Digits(integer = 4, fraction = 2) BigDecimal totalScore
) {

    public record CourseResult(
            @NotNull Long courseId,
            @NotNull @PositiveOrZero Integer correct,
            @NotNull @PositiveOrZero Integer wrong,
            @NotNull @PositiveOrZero Integer blank
    ) {
    }

    public record AreaScore(
            @NotNull Long areaId,
            @NotNull @PositiveOrZero @Digits(integer = 4, fraction = 2) BigDecimal score
    ) {
    }
}
