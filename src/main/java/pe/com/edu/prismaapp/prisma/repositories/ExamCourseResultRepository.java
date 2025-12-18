package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.com.edu.prismaapp.prisma.dto.exam.ExamEffectiveCourse;
import pe.com.edu.prismaapp.prisma.entities.ExamCourseResult;

import java.util.List;
import java.util.Optional;

public interface ExamCourseResultRepository extends JpaRepository<ExamCourseResult, Long> {
    Optional<ExamCourseResult> findExamCourseResultByCourse_IdAndExamResult_Id(Long courseId, Long examResultId);

    @Query(value = """
            SELECT new pe.com.edu.prismaapp.prisma.dto.exam.ExamEffectiveCourse(C.name,
                   SUM(CASE WHEN F.abbreviation = 'LECT' then A.courseCorrect ELSE 0 END)   ,
                   SUM(CASE WHEN F.abbreviation = 'LECT' then A.courseIncorrect ELSE 0 END) ,
            
                   SUM(CASE WHEN F.abbreviation = 'NYO' then A.courseCorrect ELSE 0 END)    ,
                   SUM(CASE WHEN F.abbreviation = 'NYO' then A.courseIncorrect ELSE 0 END)  ,
            
                   SUM(CASE WHEN F.abbreviation = 'X' then A.courseCorrect ELSE 0 END)      ,
                   SUM(CASE WHEN F.abbreviation = 'X' then A.courseIncorrect ELSE 0 END)    ,
            
                   SUM(CASE WHEN F.abbreviation = 'GEO' then A.courseCorrect ELSE 0 END)    ,
                   SUM(CASE WHEN F.abbreviation = 'GEO' then A.courseIncorrect ELSE 0 END)  ,
            
                   SUM(CASE WHEN F.abbreviation = 'TRIGO' then A.courseCorrect ELSE 0 END)  ,
                   SUM(CASE WHEN F.abbreviation = 'TRIGO' then A.courseIncorrect ELSE 0 END),
            
                   SUM(CASE WHEN F.abbreviation = 'EST' then A.courseCorrect ELSE 0 END)    ,
                   SUM(CASE WHEN F.abbreviation = 'EST' then A.courseIncorrect ELSE 0 END)  )
            
            FROM ExamCourseResult A
                     INNER JOIN A.examResult B
                     INNER JOIN A.course F
                     INNER JOIN B.exam C
                     INNER JOIN C.stage D
                     INNER JOIN B.studentStage E
            where D.cycle.id = :cycleId
              and E.student.id = :studentId
            GROUP BY C.name, C.date
            ORDER BY C.date
            """
    )
    List<ExamEffectiveCourse> getExamEffectiveCourseByStudentAndCycle(Long cycleId, Long studentId);
}