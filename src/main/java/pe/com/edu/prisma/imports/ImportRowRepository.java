package pe.com.edu.prisma.imports;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRowRepository extends JpaRepository<ImportRow, Long> {

    Optional<ImportRow> findByIdAndBatchId(Long id, Long batchId);
}
