package pe.com.edu.prisma.imports;

import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prisma.imports.dto.ImportBatchDto;

/**
 * Importing an exam's results from the academy's Excel (REQ-EXAMS-02): the
 * three roles, as the rest of the exams module. The batch is then reviewed
 * and confirmed at /import-batches.
 */
@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'TUTOR', 'COORDINADOR')")
public class ExamResultImportController {

    private final ExamResultImportService importService;

    public ExamResultImportController(ExamResultImportService importService) {
        this.importService = importService;
    }

    @PostMapping(path = "/api/v1/exams/{examId}/results/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportBatchDto> upload(@PathVariable Long examId, @RequestPart("file") MultipartFile file) {
        ImportBatchDto batch = importService.upload(examId, file);
        return ResponseEntity.created(URI.create("/api/v1/import-batches/" + batch.id())).body(batch);
    }
}
