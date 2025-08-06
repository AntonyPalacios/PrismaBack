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
    List<Object[]> getMinMaxAndAvgByExamByArea(Long examId,Long areaId);

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
            "INNER JOIN C.stage D " +
            "INNER JOIN D.cycle E " +
            "WHERE B.student.id = :studentId AND E.id = :cycleId AND F.course.id = :courseId " +
            "ORDER BY C.date ")
    List<ExamScoreDTO> listExamEffectiveByCourseByStudent(Long studentId, Long cycleId, Long courseId);
}