package pe.com.edu.prisma.imports;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prisma.imports.dto.ImportBatchDto;
import pe.com.edu.prisma.imports.dto.ResolveRowRequest;
import pe.com.edu.prisma.imports.dto.StudentRefDto;

/** Importing students from the template, through a review batch (REQ-IMPORTS-02, REQ-IMPORTS-01). */
public interface StudentImportService {

    byte[] template();

    /** Reads the file into a pending batch; nothing real is written. {@code stageId}: where to enroll (optional). */
    ImportBatchDto upload(MultipartFile file, Long stageId);

    /** Students batches, newest first; only those with {@code status} when given. Without rows. */
    List<ImportBatchDto> findAll(ImportBatchStatus status);

    ImportBatchDto findById(Long id);

    ImportBatchDto resolve(Long batchId, Long rowId, ResolveRowRequest request);

    /** Registers, enrolls and gives tutors — all or nothing; only without unresolved rows. */
    ImportBatchDto confirm(Long batchId);

    void discard(Long batchId);

    /**
     * Any registered student by name or DNI (at most 20), to resolve a row by
     * hand. Searches every student: unlike the students list, a tutor is not
     * limited to their own here, since the row may be anyone.
     */
    List<StudentRefDto> searchStudents(Long batchId, String query);
}
