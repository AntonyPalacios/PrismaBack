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

    @Query(value = """
SELECT
    C.name                                   AS exam_name,
    B.total_score                            AS score,
    ROUND(
            CASE
                WHEN G.score_goal IS NOT NULL THEN
                    prev_score * (1+G.score_goal)
                WHEN prev_score IS NOT NULL THEN
                    prev_score * 1.10
                ELSE
                    B.total_score * 1.10
                END
        , 2) AS goal,
    B.merit
FROM (
         SELECT
             B.*,
             LAG(B.total_score) OVER (
                 PARTITION BY E.id_student
                 ORDER BY C.date
                 ) AS prev_score
         FROM texamresult B
                  INNER JOIN texam C ON C.id_exam = B.id_exam
                  INNER JOIN tstage D ON D.id_stage = C.id_stage
                  INNER JOIN tstudentstage E ON E.id_student_stage = B.id_student_stage

         WHERE D.id_cycle = :cycleId
           AND E.id_student = :studentId
     ) B
         INNER JOIN texam C ON C.id_exam = B.id_exam
         INNER JOIN tstudentstage E ON E.id_student_stage = B.id_student_stage
         INNER JOIN tstudent F on E.id_student = F.id
         LEFT JOIN tgoal G
                   ON G.id_exam = C.id_exam and G.id = F.id

ORDER BY C.date
""", nativeQuery = true)
    List<ExamGoal> listExamResultsWithGoalsByStudent(Long studentId, Long cycleId);
}