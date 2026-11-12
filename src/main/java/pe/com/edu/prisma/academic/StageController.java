package pe.com.edu.prisma.academic;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.academic.dto.ChangeStageStatusRequest;
import pe.com.edu.prisma.academic.dto.StageDto;
import pe.com.edu.prisma.academic.dto.UpsertStageRequest;

/**
 * Stages of a cycle (REQ-ACADEMIC-02): any authenticated user reads them
 * (enrollment and tutoring hang from the stage); only ADMIN creates, edits,
 * changes the status and deletes (soft delete, 204).
 */
@RestController
@RequestMapping("/api/v1/cycles/{cycleId}/stages")
public class StageController {

    private final StageService stageService;

    public StageController(StageService stageService) {
        this.stageService = stageService;
    }

    @GetMapping
    public List<StageDto> findAll(@PathVariable Long cycleId) {
        return stageService.findAll(cycleId);
    }

    @GetMapping("/{id}")
    public StageDto findById(@PathVariable Long cycleId, @PathVariable Long id) {
        return stageService.findById(cycleId, id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StageDto> create(@PathVariable Long cycleId, @Valid @RequestBody UpsertStageRequest request) {
        StageDto created = stageService.create(cycleId, request);
        URI location = URI.create("/api/v1/cycles/" + cycleId + "/stages/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StageDto update(@PathVariable Long cycleId, @PathVariable Long id,
            @Valid @RequestBody UpsertStageRequest request) {
        return stageService.update(cycleId, id, request);
    }

    // PLANNED → ACTIVE → CLOSED, forward only
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public StageDto changeStatus(@PathVariable Long cycleId, @PathVariable Long id,
            @Valid @RequestBody ChangeStageStatusRequest request) {
        return stageService.changeStatus(cycleId, id, request);
    }

    // soft delete; only while the stage has no enrollments and no exams
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long cycleId, @PathVariable Long id) {
        stageService.delete(cycleId, id);
        return ResponseEntity.noContent().build();
    }
}
