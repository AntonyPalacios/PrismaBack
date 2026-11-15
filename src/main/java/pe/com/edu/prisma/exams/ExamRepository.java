package pe.com.edu.prisma.exams;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findByIdAndMockExamId(Long id, Long mockExamId);

    boolean existsByMockExamIdAndExamTypeId(Long mockExamId, Long examTypeId);

    // ids of the given exams that already have results
    @Query(value = "select distinct exam_id from exam_result where exam_id in (:examIds)", nativeQuery = true)
    List<Long> findIdsWithResults(@Param("examIds") Collection<Long> examIds);
}
