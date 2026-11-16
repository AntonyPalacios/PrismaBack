package pe.com.edu.prisma.exams;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    boolean existsByExamId(Long examId);

    /** A student's results in the exams they sat, with exam, mock exam, stage and cycle loaded. */
    @Query("""
            select r from ExamResult r
            join fetch r.exam e
            join fetch e.examType
            join fetch e.mockExam m
            join fetch m.stage s
            join fetch s.cycle
            where r.enrollment.student.id = :studentId and r.attended = true and r.totalScore is not null
            """)
    List<ExamResult> findAttendedByStudentId(@Param("studentId") Long studentId);

    /** The total score of everyone who sat each of these exams. */
    @Query("""
            select r.exam.id as examId, r.totalScore as totalScore from ExamResult r
            where r.exam.id in :examIds and r.attended = true and r.totalScore is not null
            """)
    List<ExamScore> findAttendedScoresByExamIds(@Param("examIds") Collection<Long> examIds);

    interface ExamScore {

        Long getExamId();

        BigDecimal getTotalScore();
    }
}
