package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.com.edu.prismaapp.prisma.dto.exam.ExamGoal;
import pe.com.edu.prismaapp.prisma.dto.exam.ExamScore;
import pe.com.edu.prismaapp.prisma.dto.exam.ExamSectionSummary;
import pe.com.edu.prismaapp.prisma.entities.ExamResult;

import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    Optional<ExamResult> findExamResultByExam_IdAndStudentStage_Id(Long examId, Long studentStageId);

    @Query("""
    SELECT new pe.com.edu.prismaapp.prisma.dto.exam.ExamScore(
        C.name,
        B.totalScore,
        B.merit,
        MIN(F.totalScore),
        MAX(F.totalScore),
        ROUND(AVG(F.totalScore),2))
    from ExamResult B
        INNER JOIN B.exam C
        INNER JOIN ExamResult F on (F.exam = C and F.area = B.area)
        INNER JOIN C.stage D
        INNER JOIN B.studentStage E
    where
        D.cycle.id = :cycleId
        AND E.student.id = :studentId
    GROUP BY C.name,C.date, B.totalScore, B.merit
    ORDER BY C.date
""")
    List<ExamScore> listExamResultsByStudent(Long studentId, Long cycleId);

    @Query("""
    SELECT new pe.com.edu.prismaapp.prisma.dto.exam.ExamSectionSummary(
        C.id,
        MIN(A.courseCorrect),
        MIN(A.courseIncorrect),
        MAX(A.courseCorrect),
        MAX(A.courseIncorrect),
        ROUND(AVG(A.courseCorrect),0),
        ROUND(AVG(A.courseIncorrect),0))
    FROM ExamCourseResult A
        INNER JOIN A.examResult B
        INNER JOIN B.exam C
        INNER JOIN C.stage D
        INNER JOIN B.studentStage E
        INNER JOIN A.course F
        INNER JOIN StudentStageUser G on G.studentStage = E
    WHERE
        F.parentCourse.id = :sectionId
        AND D.cycle.id = :cycleId
        AND G.user.id = :userId
        AND (:areaId is null OR B.area.id = :areaId)
        AND C.id in (:examIds)
    GROUP BY C.id, C.date
    ORDER BY C.date
""")
    List<ExamSectionSummary> getExamSummaryByTutor(List<Long> examIds, Long areaId, Long userId, Long cycleId, Long sectionId);

    @Query("""
    SELECT new pe.com.edu.prismaapp.prisma.dto.exam.ExamGoal(
        C.name,
        B.totalScore,
        ROUND(B.totalScore * (1 + coalesce(G.scoreGoal,0.1)),2),
        B.merit)
    from ExamResult B
        INNER JOIN B.exam C
        INNER JOIN C.stage D
        INNER JOIN B.studentStage E
        INNER JOIN E.student F
        LEFT JOIN Goal G on (G.exam = C and G.student = F)
    where
        D.cycle.id = :cycleId
        AND F.id = :studentId
    GROUP BY C.name,C.date, B.totalScore, B.merit,ROUND(B.totalScore * (1 + coalesce(G.scoreGoal,0.1)),2)
    ORDER BY C.date
""")
    List<ExamGoal> listExamResultsWithGoalsByStudent(Long studentId, Long cycleId);
}