package pe.com.edu.prisma.imports;

import java.math.BigDecimal;
import java.util.List;

/**
 * A row of an exam results file (sheet EVA) as it was read, plus what the
 * review decided. {@code fullName} is the single "APELLIDOS Y NOMBRES" cell.
 * {@code enrolls}: the student is not enrolled in the exam's stage and will
 * be when the batch is confirmed. {@code newStudent}: the names a person
 * split by hand to register the row as a new student. {@code tutorText} is
 * the file's TUTOR cell and {@code tutorId} the active tutor it matched when
 * uploading (null = none or several): the tutor given on confirm to whoever
 * has none. {@code notice}
 * explains why the row needs a person. Everything here is personal data or
 * scores: it never goes to the logs.
 */
public record ResultRowPayload(
        String fullName,
        String dni,
        boolean attended,
        List<CourseCount> courses,
        List<AreaScore> areas,
        BigDecimal totalScore,
        String tutorText,
        Long tutorId,
        boolean enrolls,
        NewStudent newStudent,
        String notice
) {

    /** Correct, wrong and blank answers of one course. */
    public record CourseCount(Long courseId, int correct, int wrong, int blank) {
    }

    /** One area's score, as the file brings it. */
    public record AreaScore(Long areaId, BigDecimal score) {
    }

    public record NewStudent(String firstName, String lastNamePat, String lastNameMat) {
    }

    public ResultRowPayload withReview(boolean enrolls, NewStudent newStudent, String notice) {
        return new ResultRowPayload(fullName, dni, attended, courses, areas, totalScore, tutorText, tutorId, enrolls,
                newStudent, notice);
    }

    public int totalCorrect() {
        return courses.stream().mapToInt(CourseCount::correct).sum();
    }

    public int totalWrong() {
        return courses.stream().mapToInt(CourseCount::wrong).sum();
    }

    public int totalBlank() {
        return courses.stream().mapToInt(CourseCount::blank).sum();
    }
}
