package pe.com.edu.prisma.logbook;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.logbook.dto.CreateLogEntryRequest;
import pe.com.edu.prisma.logbook.dto.LogEntryDto;
import pe.com.edu.prisma.logbook.dto.LogEntryPageDto;

/**
 * A student's logbook (REQ-LOGBOOK-01/02, agreements of 2026-11-17): ADMIN
 * and the student's current tutor read it; only the current tutor writes.
 * There is no update or delete, by design. The tutor checks are in the service.
 */
@RestController
@RequestMapping("/api/v1/students/{studentId}/logbook-entries")
public class LogbookController {

    private final LogbookService logbookService;

    public LogbookController(LogbookService logbookService) {
        this.logbookService = logbookService;
    }

    // page from 0; size between 1 and 100 (20 by default), clamped in the service
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
    public LogEntryPageDto findByStudent(@PathVariable Long studentId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return logbookService.findByStudent(studentId, page, size);
    }

    @PostMapping
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<LogEntryDto> create(@PathVariable Long studentId, @Valid @RequestBody CreateLogEntryRequest request) {
        LogEntryDto created = logbookService.create(studentId, request);
        return ResponseEntity.created(URI.create("/api/v1/students/" + studentId + "/logbook-entries/" + created.id()))
                .body(created);
    }
}
