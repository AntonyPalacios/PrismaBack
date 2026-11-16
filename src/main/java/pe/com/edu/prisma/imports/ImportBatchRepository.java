package pe.com.edu.prisma.imports;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    List<ImportBatch> findByTypeOrderByUploadedAtDescIdDesc(ImportBatchType type);

    List<ImportBatch> findByTypeAndStatusOrderByUploadedAtDescIdDesc(ImportBatchType type, ImportBatchStatus status);
}
