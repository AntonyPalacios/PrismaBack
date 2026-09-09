package pe.com.edu.prisma.catalog.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.catalog.CourseRepository;
import pe.com.edu.prisma.catalog.ExamType;
import pe.com.edu.prisma.catalog.ExamTypeRepository;
import pe.com.edu.prisma.catalog.ExamTypeService;
import pe.com.edu.prisma.catalog.dto.ExamTypeDto;
import pe.com.edu.prisma.catalog.dto.UpsertExamTypeRequest;
import pe.com.edu.prisma.catalog.mapper.ExamTypeMapper;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class ExamTypeServiceImpl implements ExamTypeService {

    private final ExamTypeRepository examTypeRepository;
    private final CourseRepository courseRepository;
    private final ExamTypeMapper examTypeMapper;

    public ExamTypeServiceImpl(ExamTypeRepository examTypeRepository, CourseRepository courseRepository,
            ExamTypeMapper examTypeMapper) {
        this.examTypeRepository = examTypeRepository;
        this.courseRepository = courseRepository;
        this.examTypeMapper = examTypeMapper;
    }

    @Override
    public List<ExamTypeDto> findAll() {
        return examTypeRepository.findAll().stream().map(examTypeMapper::toDto).toList();
    }

    @Override
    public ExamTypeDto findById(Long id) {
        return examTypeMapper.toDto(getOrThrow(id));
    }

    @Override
    @Transactional
    public ExamTypeDto create(UpsertExamTypeRequest request) {
        ExamType examType = new ExamType();
        examType.setName(request.name());
        return examTypeMapper.toDto(examTypeRepository.save(examType));
    }

    @Override
    @Transactional
    public ExamTypeDto update(Long id, UpsertExamTypeRequest request) {
        ExamType examType = getOrThrow(id);
        examType.setName(request.name());
        return examTypeMapper.toDto(examType);
    }

    @Override
    @Transactional
    public ExamTypeDto assignCourses(Long id, Set<Long> courseIds) {
        ExamType examType = getOrThrow(id);
        Set<pe.com.edu.prisma.catalog.Course> courses = new HashSet<>(courseRepository.findAllById(courseIds));
        if (courses.size() != courseIds.size()) {
            throw new BusinessRuleException("Uno o más cursos seleccionados no existen.");
        }
        examType.setCourses(courses);
        return examTypeMapper.toDto(examType);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        getOrThrow(id).setActive(false);
    }

    private ExamType getOrThrow(Long id) {
        return examTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el tipo de examen solicitado."));
    }
}
