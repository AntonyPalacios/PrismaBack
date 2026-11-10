package pe.com.edu.prisma.catalog.impl;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.catalog.Course;
import pe.com.edu.prisma.catalog.CourseRepository;
import pe.com.edu.prisma.catalog.ExamType;
import pe.com.edu.prisma.catalog.ExamTypeRepository;
import pe.com.edu.prisma.catalog.ExamTypeService;
import pe.com.edu.prisma.catalog.dto.ExamTypeDto;
import pe.com.edu.prisma.catalog.dto.UpsertExamTypeRequest;
import pe.com.edu.prisma.catalog.mapper.ExamTypeMapper;
import pe.com.edu.prisma.common.SpanishOrder;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class ExamTypeServiceImpl implements ExamTypeService {

    static final String DUPLICATE_NAME = "Ya existe un tipo de examen con ese nombre.";
    static final String UNKNOWN_COURSE = "Uno o más cursos seleccionados no existen.";

    private static final Comparator<ExamType> ORDER =
            SpanishOrder.<ExamType>by(ExamType::getName).thenComparing(ExamType::getId);

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
        return examTypeRepository.findAll().stream().sorted(ORDER).map(examTypeMapper::toDto).toList();
    }

    @Override
    public ExamTypeDto findById(Long id) {
        return examTypeMapper.toDto(getOrThrow(id));
    }

    @Override
    @Transactional
    public ExamTypeDto create(UpsertExamTypeRequest request) {
        String name = request.name().trim();
        if (examTypeRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException(DUPLICATE_NAME);
        }
        ExamType examType = new ExamType();
        examType.setName(name);
        examType.setCourses(resolveCourses(request.courseIds()));
        return examTypeMapper.toDto(examTypeRepository.save(examType));
    }

    @Override
    @Transactional
    public ExamTypeDto update(Long id, UpsertExamTypeRequest request) {
        ExamType examType = getOrThrow(id);
        String name = request.name().trim();
        if (examTypeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessRuleException(DUPLICATE_NAME);
        }
        examType.setName(name);
        // full replace, never incremental: the new set is the whole truth
        examType.setCourses(resolveCourses(request.courseIds()));
        return examTypeMapper.toDto(examType);
    }

    /** All or nothing: one unknown id rejects the whole request before anything changes. */
    private Set<Course> resolveCourses(Set<Long> courseIds) {
        Set<Course> courses = new HashSet<>(courseRepository.findAllById(courseIds));
        if (courses.size() != courseIds.size()) {
            throw new BusinessRuleException(UNKNOWN_COURSE);
        }
        return courses;
    }

    private ExamType getOrThrow(Long id) {
        return examTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el tipo de examen solicitado."));
    }
}
