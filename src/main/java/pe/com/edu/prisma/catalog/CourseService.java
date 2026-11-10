package pe.com.edu.prisma.catalog;

import java.util.List;
import pe.com.edu.prisma.catalog.dto.CourseDto;

/** Courses are fixed seed data: read-only (project agreement, 2026-11-10). */
public interface CourseService {

    List<CourseDto> findAll();

    List<CourseDto> findByArea(Long areaId);

    CourseDto findById(Long id);
}
