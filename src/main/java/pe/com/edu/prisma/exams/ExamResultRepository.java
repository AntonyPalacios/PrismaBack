package pe.com.edu.prisma.exams;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    boolean existsByExamId(Long examId);
}
