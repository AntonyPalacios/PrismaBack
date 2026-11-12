package pe.com.edu.prisma.academic;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.academic.dto.ChangeCycleStatusRequest;
import pe.com.edu.prisma.academic.dto.CycleDto;
import pe.com.edu.prisma.academic.dto.UpsertCycleRequest;

/**
 * Academic cycles (REQ-ACADEMIC-01): any authenticated user reads them
 * (the rest of the system hangs from the cycle); only ADMIN creates, edits
 * and changes the status. There is no delete: a cycle is closed, never removed.
 */
@RestController
@RequestMapping("/api/v1/cycles")
public class CycleController {

    private final CycleService cycleService;

    public CycleController(CycleService cycleService) {
        this.cycleService = cycleService;
    }

    @GetMapping
    public List<CycleDto> findAll() {
        return cycleService.findAll();
    }

    @GetMapping("/{id}")
    public CycleDto findById(@PathVariable Long id) {
        return cycleService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CycleDto> create(@Valid @RequestBody UpsertCycleRequest request) {
        CycleDto created = cycleService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/cycles/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CycleDto update(@PathVariable Long id, @Valid @RequestBody UpsertCycleRequest request) {
        return cycleService.update(id, request);
    }

    // PLANNED → ACTIVE → CLOSED, forward only
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public CycleDto changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeCycleStatusRequest request) {
        return cycleService.changeStatus(id, request);
    }
}
