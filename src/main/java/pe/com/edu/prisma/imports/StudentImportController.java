package pe.com.edu.prisma.imports;

import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prisma.imports.dto.ImportBatchDto;

/**
 * Importing students from Excel (REQ-IMPORTS-02): ADMIN and TUTOR, who are
 * the ones registering students; the batch is then reviewed and confirmed
 * at /import-batches.
 */
@RestController
@RequestMapping("/api/v1/student-imports")
@PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
public class StudentImportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final StudentImportService importService;

    public StudentImportController(StudentImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-alumnos.xlsx\"")
                .body(importService.template());
    }

    // stageId (optional): where the students will be enrolled when the batch is confirmed
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportBatchDto> upload(@RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long stageId) {
        ImportBatchDto batch = importService.upload(file, stageId);
        return ResponseEntity.created(URI.create("/api/v1/import-batches/" + batch.id())).body(batch);
    }
}
