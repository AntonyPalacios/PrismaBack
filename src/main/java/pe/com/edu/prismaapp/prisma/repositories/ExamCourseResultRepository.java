package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.edu.prismaapp.prisma.entities.ExamCourseResult;

import java.util.Optional;

public interface ExamCourseResultRepository extends JpaRepository<ExamCourseResult, Long> {
    Optional<ExamCourseResult> findExamCourseResultByCourse_IdAndExamResult_Id(Long courseId, Long examResultId);
}