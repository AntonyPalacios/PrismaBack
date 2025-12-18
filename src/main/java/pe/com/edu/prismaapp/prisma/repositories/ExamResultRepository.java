package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.com.edu.prismaapp.prisma.dto.exam.ExamScore;
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
             where D.cycle.id = :cycleId
               and E.student.id = :studentId
             GROUP BY C.name,C.date, B.totalScore, B.merit
             ORDER BY C.date
            """)
    List<ExamScore> listExamResultsByStudent(Long studentId, Long cycleId);

    @Query(value = "SELECT d.id_exam, min(t.sum_correct), min(t.sum_incorrect) " +
            "FROM texam d " +
            "INNER JOIN ( " +
            "    SELECT " +
            "        a.id_exam, " +
            "        SUM(f.course_correct) AS sum_correct, " +
            "        SUM(f.course_incorrect) AS sum_incorrect " +
            "    FROM texamcourseresult f " +
            "    INNER JOIN texamresult a ON f.id_exam_result = a.id_exam_result " +
            "    INNER JOIN tstudentstage b ON a.id_student_stage = b.id_student_stage " +
            "    INNER JOIN tstudentstageuser c ON b.id_student_stage = c.id_student_stage " +
            "    INNER JOIN tcourse g ON f.id_course = g.id_course " +
            "    WHERE g.id_parent_course = :courseId and a.id_exam = :examId " +
            "    AND (:areaId IS NULL OR a.id_area = :areaId) and c.id_user= :userId " +
            "    GROUP BY a.id_exam_result " +
            ") AS t ON d.id_exam = t.id_exam " +
            "GROUP BY d.id_exam, d.name, d.date " +
            "ORDER BY d.date;",
            nativeQuery = true)
    List<Object[]> getMinExamResult(Long examId, Long areaId, Long userId, Long courseId);

    @Query(value = "SELECT d.id_exam, max(t.sum_correct), max(t.sum_incorrect) " +
            "FROM texam d " +
            "INNER JOIN ( " +
            "    SELECT " +
            "        a.id_exam, " +
            "        SUM(f.course_correct) AS sum_correct, " +
            "        SUM(f.course_incorrect) AS sum_incorrect " +
            "    FROM texamcourseresult f " +
            "    INNER JOIN texamresult a ON f.id_exam_result = a.id_exam_result " +
            "    INNER JOIN tstudentstage b ON a.id_student_stage = b.id_student_stage " +
            "    INNER JOIN tstudentstageuser c ON b.id_student_stage = c.id_student_stage " +
            "    INNER JOIN tcourse g ON f.id_course = g.id_course " +
            "    WHERE g.id_parent_course = :courseId and a.id_exam = :examId " +
            "    AND (:areaId IS NULL OR a.id_area = :areaId) and c.id_user= :userId " +
            "    GROUP BY a.id_exam_result " +
            ") AS t ON d.id_exam = t.id_exam " +
            "GROUP BY d.id_exam, d.name, d.date " +
            "ORDER BY d.date;",
            nativeQuery = true)
    List<Object[]> getMaxExamResult(Long examId, Long areaId, Long userId, Long courseId);


    @Query(value = "SELECT d.id_exam, round(avg(t.sum_correct)) , round(avg(t.sum_incorrect)) " +
            "FROM texam d " +
            "INNER JOIN ( " +
            "    SELECT " +
            "        a.id_exam, " +
            "        SUM(f.course_correct) AS sum_correct, " +
            "        SUM(f.course_incorrect) AS sum_incorrect " +
            "    FROM texamcourseresult f " +
            "    INNER JOIN texamresult a ON f.id_exam_result = a.id_exam_result " +
            "    INNER JOIN tstudentstage b ON a.id_student_stage = b.id_student_stage " +
            "    INNER JOIN tstudentstageuser c ON b.id_student_stage = c.id_student_stage " +
            "    INNER JOIN tcourse g ON f.id_course = g.id_course " +
            "    WHERE g.id_parent_course = :courseId and a.id_exam = :examId " +
            "    AND (:areaId IS NULL OR a.id_area = :areaId) and c.id_user= :userId " +
            "    GROUP BY a.id_exam_result " +
            ") AS t ON d.id_exam = t.id_exam " +
            "GROUP BY d.id_exam, d.name, d.date " +
            "ORDER BY d.date;",
            nativeQuery = true)
    List<Object[]> getAvgExamResult(Long examId, Long areaId, Long userId, Long courseId);
}