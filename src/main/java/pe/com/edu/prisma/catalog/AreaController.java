package pe.com.edu.prisma.catalog;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.catalog.dto.AreaDto;
import pe.com.edu.prisma.catalog.dto.UpsertAreaRequest;

@RestController
@RequestMapping("/api/v1/areas")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @GetMapping
    public List<AreaDto> findAll() {
        return areaService.findAll();
    }

    @GetMapping("/{id}")
    public AreaDto findById(@PathVariable Long id) {
        return areaService.findById(id);
    }

    // Every mutating catalog endpoint is admin-only: tutors consume the
    // catalog, they don't redefine it. Enforced here — not left implicit —
    // so the rule is visible in one place per endpoint, unlike the
    // ad-hoc @PreAuthorize coverage the audit found in the old backend.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AreaDto> create(@Valid @RequestBody UpsertAreaRequest request) {
        AreaDto created = areaService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/areas/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AreaDto update(@PathVariable Long id, @Valid @RequestBody UpsertAreaRequest request) {
        return areaService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        areaService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
