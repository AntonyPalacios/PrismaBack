package pe.com.edu.prisma.exams;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    @EntityGraph(attributePaths = {"examType", "stage", "stage.cycle"})
    List<Exam> findByStageId(Long stageId);

    boolean existsByStageIdAndExamTypeIdAndNameIgnoreCase(Long stageId, Long examTypeId, String name);

    boolean existsByStageIdAndExamTypeIdAndNameIgnoreCaseAndIdNot(Long stageId, Long examTypeId, String name, Long id);

    @Query(value = "select count(*) from exam_result where exam_id = :examId", nativeQuery = true)
    long countResults(@Param("examId") Long examId);

    // ids of the given exams that already have results
    @Query(value = "select distinct exam_id from exam_result where exam_id in (:examIds)", nativeQuery = true)
    List<Long> findIdsWithResults(@Param("examIds") List<Long> examIds);
}
