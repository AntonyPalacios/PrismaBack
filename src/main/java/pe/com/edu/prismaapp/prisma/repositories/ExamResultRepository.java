package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.edu.prismaapp.prisma.entities.ExamResult;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
}