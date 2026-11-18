package pe.com.edu.prisma.goals;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.exams.ExamResult;
import pe.com.edu.prisma.exams.MockExam;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    @Query("select g from Goal g where g.enrollment.id in :enrollmentIds and g.targetExam.mockExam.id = :mockExamId")
    List<Goal> findByEnrollmentsAndMockExam(@Param("enrollmentIds") Collection<Long> enrollmentIds,
            @Param("mockExamId") Long mockExamId);

    /** A student's goals aiming at exams of a cycle with that status, with exam, type and mock exam loaded. */
    @Query("""
            select g from Goal g
            join fetch g.targetExam e
            join fetch e.examType
            join fetch e.mockExam m
            where g.enrollment.student.id = :studentId and m.stage.cycle.status = :cycleStatus
            """)
    List<Goal> findByStudentAndCycleStatus(@Param("studentId") Long studentId,
            @Param("cycleStatus") CycleStatus cycleStatus);

    /** Every result of these students, any cycle, with exam, type, mock exam and course counts loaded. */
    @Query("""
            select distinct r from ExamResult r
            join fetch r.exam e
            join fetch e.examType
            join fetch e.mockExam
            join fetch r.enrollment en
            left join fetch r.courses c
            left join fetch c.course
            where en.student.id in :studentIds
            """)
    List<ExamResult> findResultsOfStudents(@Param("studentIds") Collection<Long> studentIds);

    /** The mock exams of the cycles with that status, with their stage. */
    @Query("select m from MockExam m join fetch m.stage s where s.cycle.status = :cycleStatus")
    List<MockExam> findMockExamsByCycleStatus(@Param("cycleStatus") CycleStatus cycleStatus);
}
