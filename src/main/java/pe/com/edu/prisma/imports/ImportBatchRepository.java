package pe.com.edu.prisma.imports;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    List<ImportBatch> findByTypeOrderByUploadedAtDescIdDesc(ImportBatchType type);

    List<ImportBatch> findByTypeAndStatusOrderByUploadedAtDescIdDesc(ImportBatchType type, ImportBatchStatus status);

    // the batch in review of an exam, if any (at most one: a second upload is stopped)
    java.util.Optional<ImportBatch> findFirstByExamIdAndStatusOrderByIdDesc(Long examId, ImportBatchStatus status);
}
