package pe.com.edu.prisma.catalog;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.catalog.dto.AreaDto;

/**
 * Read-only: the academy always has the same 2 areas, seeded by Flyway.
 * Any change to them is a new migration, not an API call
 * (project agreement, 2026-11-10).
 */
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
}
