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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.catalog.dto.CourseDto;
import pe.com.edu.prisma.catalog.dto.UpsertCourseRequest;

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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseDto> create(@Valid @RequestBody UpsertCourseRequest request) {
        CourseDto created = courseService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/courses/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseDto update(@PathVariable Long id, @Valid @RequestBody UpsertCourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        courseService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
