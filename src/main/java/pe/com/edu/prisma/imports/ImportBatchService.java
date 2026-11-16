package pe.com.edu.prisma.imports;

import java.util.List;
import pe.com.edu.prisma.imports.dto.ImportBatchDto;
import pe.com.edu.prisma.imports.dto.ResolveRowRequest;
import pe.com.edu.prisma.imports.dto.StudentRefDto;

/**
 * The review queue of every import (REQ-IMPORTS-01), whatever the batch
 * type: hands each batch to its own import. Students batches are only for
 * ADMIN and TUTOR (403 otherwise); exam results batches also for COORDINADOR.
 */
public interface ImportBatchService {

    /** The batches the user may see, newest first, without rows. */
    List<ImportBatchDto> findAll(ImportBatchStatus status);

    ImportBatchDto findById(Long id);

    ImportBatchDto resolve(Long batchId, Long rowId, ResolveRowRequest request);

    ImportBatchDto confirm(Long batchId);

    void discard(Long batchId);

    /**
     * Any registered student by name or DNI (at most 20), to resolve a row by
     * hand. Searches every student: unlike the students list, a tutor is not
     * limited to their own here, since the row may be anyone.
     */
    List<StudentRefDto> searchStudents(Long batchId, String query);
}
