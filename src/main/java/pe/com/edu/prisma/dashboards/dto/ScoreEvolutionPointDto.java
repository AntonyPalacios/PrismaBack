package pe.com.edu.prisma.dashboards.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One exam of a student's score evolution (REQ-DASHBOARDS-01).
 * {@code examName} is the mock exam's name. {@code merit} of {@code meritOf}:
 * the student's position among everyone who sat that exam (ties share it).
 * {@code examMax} / {@code examMin}: the highest and lowest total of that
 * exam. Scores are the stored ones, never recalculated.
 */
public record ScoreEvolutionPointDto(
        Long examId,
        String examName,
        String examTypeName,
        LocalDate examDate,
        String cycleName,
        BigDecimal studentScore,
        int merit,
        int meritOf,
        BigDecimal examMax,
        BigDecimal examMin,
        List<AreaScoreDto> areaScores
) {

    public record AreaScoreDto(Long areaId, String areaName, BigDecimal score) {
    }
}
