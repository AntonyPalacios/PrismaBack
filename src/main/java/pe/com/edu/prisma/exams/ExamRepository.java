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

    // the results import in review of each of the given exams (at most one each)
    @Query(value = "select exam_id as examId, max(id) as batchId from import_batch "
            + "where status = 'PENDING_REVIEW' and exam_id in (:examIds) group by exam_id", nativeQuery = true)
    List<PendingImport> findPendingImports(@Param("examIds") Collection<Long> examIds);

    interface PendingImport {

        Long getExamId();

        Long getBatchId();
    }
}
