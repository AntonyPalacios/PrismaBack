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
import pe.com.edu.prisma.catalog.dto.CareerDto;
import pe.com.edu.prisma.catalog.dto.UpsertCareerRequest;

/**
 * Careers (REQ-CATALOG-04): any authenticated user reads them (enrollment
 * needs the list); only ADMIN creates, edits, deactivates and reactivates.
 */
@RestController
@RequestMapping("/api/v1/careers")
public class CareerController {

    private final CareerService careerService;

    public CareerController(CareerService careerService) {
        this.careerService = careerService;
    }

    @GetMapping
    public List<CareerDto> findAll() {
        return careerService.findAll();
    }

    @GetMapping("/{id}")
    public CareerDto findById(@PathVariable Long id) {
        return careerService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CareerDto> create(@Valid @RequestBody UpsertCareerRequest request) {
        CareerDto created = careerService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/careers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CareerDto update(@PathVariable Long id, @Valid @RequestBody UpsertCareerRequest request) {
        return careerService.update(id, request);
    }

    // soft-delete: the row stays, listed as inactive
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        careerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public CareerDto reactivate(@PathVariable Long id) {
        return careerService.reactivate(id);
    }
}
