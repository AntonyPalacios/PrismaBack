package pe.com.edu.prisma.catalog;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.catalog.dto.CourseDto;

/**
 * Read-only: the academy always has the same 6 courses, seeded by Flyway.
 * Any change to them is a new migration, not an API call
 * (project agreement, 2026-11-10).
 */
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseDto> findAll(@RequestParam(required = false) Long areaId) {
        return areaId == null ? courseService.findAll() : courseService.findByArea(areaId);
    }

    @GetMapping("/{id}")
    public CourseDto findById(@PathVariable Long id) {
        return courseService.findById(id);
    }
}
