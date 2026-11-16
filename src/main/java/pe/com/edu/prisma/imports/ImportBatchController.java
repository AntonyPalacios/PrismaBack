package pe.com.edu.prisma.imports;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.imports.dto.ImportBatchDto;
import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;
import pe.com.edu.prisma.imports.dto.ResolveRowRequest;
import pe.com.edu.prisma.imports.dto.StudentRefDto;

/**
 * The review queue of an import (REQ-IMPORTS-01): list batches, resolve
 * rows, confirm (all or nothing) or discard. Exam results batches are for
 * the three roles; students batches only for ADMIN and TUTOR (checked by
 * the service, since it depends on the batch).
 */
@RestController
@RequestMapping("/api/v1/import-batches")
@PreAuthorize("hasAnyRole('ADMIN', 'TUTOR', 'COORDINADOR')")
public class ImportBatchController {

    private final ImportBatchService importService;

    public ImportBatchController(ImportBatchService importService) {
        this.importService = importService;
    }

    // newest first, without their rows
    @GetMapping
    public List<ImportBatchDto> findAll(@RequestParam(required = false) ImportBatchStatus status) {
        return importService.findAll(status);
    }

    @GetMapping("/{id}")
    public ImportBatchDto findById(@PathVariable Long id) {
        return importService.findById(id);
    }

    @PutMapping("/{id}/rows/{rowId}")
    public ImportBatchDto resolve(@PathVariable Long id, @PathVariable Long rowId,
            @Valid @RequestBody ResolveRowRequest request) {
        return importService.resolve(id, rowId, request);
    }

    @PostMapping("/{id}/confirm")
    public ImportBatchDto confirm(@PathVariable Long id) {
        return importService.confirm(id);
    }

    // what is typed is a name or a DNI: never logged
    @GetMapping("/{id}/student-search")
    public List<StudentRefDto> searchStudents(@PathVariable Long id,
            @Sensitive(LogMask.HIDDEN) @RequestParam String query) {
        return importService.searchStudents(id, query);
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<Void> discard(@PathVariable Long id) {
        importService.discard(id);
        return ResponseEntity.noContent().build();
    }
}
