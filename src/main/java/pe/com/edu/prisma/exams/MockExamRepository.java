package pe.com.edu.prisma.exams;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockExamRepository extends JpaRepository<MockExam, Long> {

    @EntityGraph(attributePaths = {"exams", "exams.examType"})
    List<MockExam> findByStageId(Long stageId);

    boolean existsByStageIdAndNameIgnoreCase(Long stageId, String name);

    boolean existsByStageIdAndNameIgnoreCaseAndIdNot(Long stageId, String name, Long id);
}
