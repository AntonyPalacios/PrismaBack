package pe.com.edu.prisma.imports;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prisma.imports.dto.ImportBatchDto;
import pe.com.edu.prisma.imports.dto.ResolveRowRequest;

/** Importing an exam's results from the academy's Excel, through a review batch (REQ-EXAMS-02, REQ-IMPORTS-01). */
public interface ExamResultImportService {

    /**
     * Reads the file into a pending batch; nothing real is written. Only an
     * exam of a mock exam in the current stage of the active cycle, while it
     * has no results — unless an admin asks for a {@code reload}: then the
     * batch replaces the exam's results when it is confirmed (REQ-EXAMS-03).
     */
    ImportBatchDto upload(Long examId, MultipartFile file, boolean reload, boolean replacePending);

    /** Exam results batches, newest first; only those with {@code status} when given. Without rows. */
    List<ImportBatchDto> findAll(ImportBatchStatus status);

    ImportBatchDto findById(Long id);

    ImportBatchDto resolve(Long batchId, Long rowId, ResolveRowRequest request);

    /**
     * Registers new students, enrolls who is missing and saves the results —
     * all or nothing. A reload batch (only an admin) first deletes the
     * exam's current results.
     */
    ImportBatchDto confirm(Long batchId);

    void discard(Long batchId);
}
