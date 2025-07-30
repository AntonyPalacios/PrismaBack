package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.edu.prismaapp.prisma.entities.ExamCourseResult;

public interface ExamCourseResultRepository extends JpaRepository<ExamCourseResult, Long> {
}