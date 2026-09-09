package pe.com.edu.prisma.catalog.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.catalog.Area;
import pe.com.edu.prisma.catalog.AreaRepository;
import pe.com.edu.prisma.catalog.Course;
import pe.com.edu.prisma.catalog.CourseRepository;
import pe.com.edu.prisma.catalog.CourseService;
import pe.com.edu.prisma.catalog.dto.CourseDto;
import pe.com.edu.prisma.catalog.dto.UpsertCourseRequest;
import pe.com.edu.prisma.catalog.mapper.CourseMapper;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final AreaRepository areaRepository;
    private final CourseMapper courseMapper;

    public CourseServiceImpl(CourseRepository courseRepository, AreaRepository areaRepository,
            CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.areaRepository = areaRepository;
        this.courseMapper = courseMapper;
    }

    @Override
    public List<CourseDto> findAll() {
        return courseRepository.findAll().stream().map(courseMapper::toDto).toList();
    }

    @Override
    public List<CourseDto> findByArea(Long areaId) {
        return courseRepository.findByAreaId(areaId).stream().map(courseMapper::toDto).toList();
    }

    @Override
    public CourseDto findById(Long id) {
        return courseMapper.toDto(getOrThrow(id));
    }

    @Override
    @Transactional
    public CourseDto create(UpsertCourseRequest request) {
        Course course = courseMapper.toEntity(request);
        course.setArea(getAreaOrThrow(request.areaId()));
        return courseMapper.toDto(courseRepository.save(course));
    }

    @Override
    @Transactional
    public CourseDto update(Long id, UpsertCourseRequest request) {
        Course course = getOrThrow(id);
        courseMapper.updateEntity(request, course);
        course.setArea(getAreaOrThrow(request.areaId()));
        return courseMapper.toDto(course);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        getOrThrow(id).setActive(false);
    }

    private Course getOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el curso solicitado."));
    }

    private Area getAreaOrThrow(Long areaId) {
        return areaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("El área seleccionada no existe."));
    }
}
