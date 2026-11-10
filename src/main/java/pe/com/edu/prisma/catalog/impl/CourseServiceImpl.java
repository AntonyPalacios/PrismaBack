package pe.com.edu.prisma.catalog.impl;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.catalog.Course;
import pe.com.edu.prisma.catalog.CourseRepository;
import pe.com.edu.prisma.catalog.CourseService;
import pe.com.edu.prisma.catalog.dto.CourseDto;
import pe.com.edu.prisma.catalog.mapper.CourseMapper;
import pe.com.edu.prisma.common.SpanishOrder;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private static final Comparator<Course> ORDER =
            SpanishOrder.<Course>by(Course::getName).thenComparing(Course::getId);

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    public CourseServiceImpl(CourseRepository courseRepository, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
    }

    @Override
    public List<CourseDto> findAll() {
        return toSortedDtos(courseRepository.findAll());
    }

    @Override
    public List<CourseDto> findByArea(Long areaId) {
        return toSortedDtos(courseRepository.findByAreaId(areaId));
    }

    @Override
    public CourseDto findById(Long id) {
        return courseRepository.findById(id)
                .map(courseMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el curso solicitado."));
    }

    private List<CourseDto> toSortedDtos(List<Course> courses) {
        return courses.stream().sorted(ORDER).map(courseMapper::toDto).toList();
    }
}
