package pe.com.edu.prisma.dashboards.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * A student's correct and wrong answers per area (Lectura, Matemática) and
 * in total, exam after exam (REQ-DASHBOARDS-03). They are counts — the sum
 * of the area's courses in that exam — never ratios.
 */
public record AreaDistributionDto(List<AreaSeries> areas, List<Point> totals) {

    public record AreaSeries(Long areaId, String areaName, List<Point> points) {
    }

    public record Point(Long examId, String examName, LocalDate examDate, String cycleName, int correct, int wrong) {
    }
}
