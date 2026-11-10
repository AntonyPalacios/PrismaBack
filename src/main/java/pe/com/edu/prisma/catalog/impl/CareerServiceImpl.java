package pe.com.edu.prisma.catalog.impl;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.catalog.Career;
import pe.com.edu.prisma.catalog.CareerRepository;
import pe.com.edu.prisma.catalog.CareerService;
import pe.com.edu.prisma.catalog.ExamType;
import pe.com.edu.prisma.catalog.ExamTypeRepository;
import pe.com.edu.prisma.catalog.dto.CareerDto;
import pe.com.edu.prisma.catalog.dto.UpsertCareerRequest;
import pe.com.edu.prisma.catalog.mapper.CareerMapper;
import pe.com.edu.prisma.common.SpanishOrder;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class CareerServiceImpl implements CareerService {

    static final String DUPLICATE_NAME = "Ya existe una carrera con ese nombre.";
    static final String UNKNOWN_EXAM_TYPE = "El tipo de examen seleccionado no existe.";

    private static final Comparator<Career> ORDER =
            SpanishOrder.<Career>by(Career::getName).thenComparing(Career::getId);

    private final CareerRepository careerRepository;
    private final ExamTypeRepository examTypeRepository;
    private final CareerMapper careerMapper;

    public CareerServiceImpl(CareerRepository careerRepository, ExamTypeRepository examTypeRepository,
            CareerMapper careerMapper) {
        this.careerRepository = careerRepository;
        this.examTypeRepository = examTypeRepository;
        this.careerMapper = careerMapper;
    }

    @Override
    public List<CareerDto> findAll() {
        return careerRepository.findAll().stream().sorted(ORDER).map(careerMapper::toDto).toList();
    }

    @Override
    public CareerDto findById(Long id) {
        return careerMapper.toDto(getOrThrow(id));
    }

    @Override
    @Transactional
    public CareerDto create(UpsertCareerRequest request) {
        String name = request.name().trim();
        if (careerRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException(DUPLICATE_NAME);
        }
        Career career = new Career();
        apply(career, name, request);
        return careerMapper.toDto(careerRepository.save(career));
    }

    @Override
    @Transactional
    public CareerDto update(Long id, UpsertCareerRequest request) {
        Career career = getOrThrow(id);
        String name = request.name().trim();
        if (careerRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessRuleException(DUPLICATE_NAME);
        }
        apply(career, name, request);
        return careerMapper.toDto(career);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        getOrThrow(id).setActive(false);
    }

    @Override
    @Transactional
    public CareerDto reactivate(Long id) {
        Career career = getOrThrow(id);
        career.setActive(true); // harmless if it was already active
        return careerMapper.toDto(career);
    }

    private void apply(Career career, String name, UpsertCareerRequest request) {
        ExamType examType = examTypeRepository.findById(request.examTypeId())
                .orElseThrow(() -> new BusinessRuleException(UNKNOWN_EXAM_TYPE));
        career.setName(name);
        career.setAbbreviation(blankToNull(request.abbreviation()));
        career.setExamType(examType);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Career getOrThrow(Long id) {
        return careerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos la carrera solicitada."));
    }
}
