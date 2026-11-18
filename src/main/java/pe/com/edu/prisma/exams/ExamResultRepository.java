package pe.com.edu.prisma.exams;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.edu.prisma.academic.CycleStatus;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    boolean existsByExamId(Long examId);

    List<ExamResult> findByExamId(Long examId);

    Optional<ExamResult> findByExamIdAndEnrollmentId(Long examId, Long enrollmentId);

    // whose result an exam already has
    @Query("select r.enrollment.id from ExamResult r where r.exam.id = :examId")
    Set<Long> findEnrollmentIdsByExamId(@Param("examId") Long examId);

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

    /**
     * The results of everyone who sat an exam of that type in a cycle with
     * that status, with their course counts, exam and enrollment loaded.
     */
    @Query("""
            select distinct r from ExamResult r
            join fetch r.exam e
            join fetch e.mockExam m
            join fetch r.enrollment
            left join fetch r.courses c
            left join fetch c.course
            where e.examType.id = :examTypeId and m.stage.cycle.status = :cycleStatus and r.attended = true
            """)
    List<ExamResult> findAttendedByExamTypeAndCycleStatus(@Param("examTypeId") Long examTypeId,
            @Param("cycleStatus") CycleStatus cycleStatus);

    /** The results of a mock exam's other exams (types), with their exam, type and student loaded. */
    @Query("""
            select r from ExamResult r
            join fetch r.exam e
            join fetch e.examType
            join fetch r.enrollment en
            where e.mockExam.id = :mockExamId and e.id <> :examId
            """)
    List<ExamResult> findInOtherExamsOfMockExam(@Param("mockExamId") Long mockExamId, @Param("examId") Long examId);

    interface ExamScore {

        Long getExamId();

        BigDecimal getTotalScore();
    }
}
