package pe.com.edu.prisma.catalog;

import java.util.List;
import pe.com.edu.prisma.catalog.dto.CourseDto;
import pe.com.edu.prisma.catalog.dto.UpsertCourseRequest;

public interface CourseService {

    List<CourseDto> findAll();

    List<CourseDto> findByArea(Long areaId);

    CourseDto findById(Long id);

    CourseDto create(UpsertCourseRequest request);

    CourseDto update(Long id, UpsertCourseRequest request);

    void deactivate(Long id);
}
