package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.com.edu.prismaapp.prisma.dto.ExamResultDTO;
import pe.com.edu.prismaapp.prisma.dto.ExamScoreDTO;
import pe.com.edu.prismaapp.prisma.entities.ExamResult;

import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    Optional<ExamResult> findExamResultByExam_IdAndStudentStage_Id(Long examId, Long studentStageId);


    @Query("SELECT new pe.com.edu.prismaapp.prisma.dto.ExamResultDTO(A.exam.id, A.area.id, C.name, A.merit, A.totalScore) " +
            "FROM ExamResult A " +
            "INNER JOIN A.studentStage B " +
            "INNER JOIN A.exam C " +
            "INNER JOIN C.stage D " +
            "INNER JOIN D.cycle E " +
            "WHERE B.student.id = :studentId AND E.id = :cycleId " +
            "ORDER BY C.date ")
    List<ExamResultDTO> listExamResultsByStudent(Long studentId, Long cycleId);

    @Query("SELECT min(totalScore), max(totalScore), avg(totalScore) " +
            "from ExamResult " +
            "WHERE exam.id = :examId and area.id= :areaId ")
    List<Object[]> getMinMaxAndAvgByExamByArea(Long examId, Long areaId);

    @Query("SELECT new pe.com.edu.prismaapp.prisma.dto.ExamScoreDTO(C.name, sum(F.courseCorrect), sum(F.courseIncorrect)) " +
            "FROM ExamCourseResult F " +
            "INNER JOIN F.examResult A " +
            "INNER JOIN A.studentStage B " +
            "INNER JOIN A.exam C " +
            "INNER JOIN C.stage D " +
            "INNER JOIN D.cycle E " +
            "WHERE B.student.id = :studentId AND E.id = :cycleId AND F.course.parentCourse.id = :courseId " +
            "GROUP BY C.name, C.date " +
            "ORDER BY C.date ")
    List<ExamScoreDTO> listExamEffectiveByStudent(Long studentId, Long cycleId, Long courseId);

    @Query("SELECT new pe.com.edu.prismaapp.prisma.dto.ExamScoreDTO(C.name, F.courseCorrect, F.courseIncorrect) " +
            "FROM ExamCourseResult F " +
            "INNER JOIN F.examResult A " +
            "INNER JOIN A.studentStage B " +
            "INNER JOIN A.exam C " +
            "WHERE B.student.id = :studentId AND F.course.id = :courseId AND A.exam.id = :examId " +
            "ORDER BY C.date ")
    List<ExamScoreDTO> listExamEffectiveByCourseByStudent(Long studentId, Long courseId, Long examId);

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