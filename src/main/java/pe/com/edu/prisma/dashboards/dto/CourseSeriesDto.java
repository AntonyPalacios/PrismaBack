package pe.com.edu.prisma.dashboards.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * A course's correct, wrong and blank answers of a student, exam after exam
 * (REQ-DASHBOARDS-02). {@code points} only has the exams where the course
 * was evaluated: it is never filled with zeros.
 */
public record CourseSeriesDto(Long courseId, String courseName, List<Point> points) {

    public record Point(Long examId, String examName, LocalDate examDate, String cycleName, int correct, int wrong,
            int blank) {
    }
}
