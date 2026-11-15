package pe.com.edu.prisma.exams.impl;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.academic.StageRepository;
import pe.com.edu.prisma.catalog.ExamType;
import pe.com.edu.prisma.catalog.ExamTypeRepository;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.exams.Exam;
import pe.com.edu.prisma.exams.ExamRepository;
import pe.com.edu.prisma.exams.MockExam;
import pe.com.edu.prisma.exams.MockExamRepository;
import pe.com.edu.prisma.exams.MockExamService;
import pe.com.edu.prisma.exams.dto.AddExamTypeRequest;
import pe.com.edu.prisma.exams.dto.CreateMockExamRequest;
import pe.com.edu.prisma.exams.dto.MockExamDto;
import pe.com.edu.prisma.exams.dto.UpdateMockExamRequest;
import pe.com.edu.prisma.exams.mapper.MockExamMapper;

@Service
@Transactional(readOnly = true)
public class MockExamServiceImpl implements MockExamService {

    static final String STAGE_NOT_FOUND = "No encontramos la etapa solicitada.";
    static final String EXAM_TYPE_NOT_FOUND = "No encontramos el tipo de examen solicitado.";
    static final String MOCK_EXAM_NOT_FOUND = "No encontramos el examen solicitado.";
    static final String EXAM_NOT_FOUND = "Este examen no tiene ese tipo de examen.";
    static final String CLOSED_CYCLE = "Este ciclo está cerrado: sus exámenes ya no se pueden crear ni editar.";
    static final String OUTSIDE_STAGE = "La fecha del examen debe estar dentro de las fechas de su etapa.";
    static final String DUPLICATE_NAME = "Ya existe un examen con ese nombre en esta etapa.";
    static final String TYPE_ALREADY_THERE = "Este examen ya tiene el tipo %s.";
    static final String TYPE_HAS_RESULTS = "El tipo %s ya tiene resultados, así que no se puede quitar.";
    static final String LAST_TYPE = "Un examen necesita al menos un tipo. Si sobra, elimina el examen.";
    static final String HAS_RESULTS = "Este examen ya tiene resultados, así que no se puede eliminar.";

    private static final Logger log = LoggerFactory.getLogger(MockExamServiceImpl.class);

    // newest first
    private static final Comparator<MockExam> ORDER = Comparator.comparing(MockExam::getExamDate).reversed()
            .thenComparing(mockExam -> mockExam.getName().toLowerCase())
            .thenComparing(MockExam::getId);

    private final MockExamRepository mockExamRepository;
    private final ExamRepository examRepository;
    private final StageRepository stageRepository;
    private final ExamTypeRepository examTypeRepository;
    private final MockExamMapper mapper;

    public MockExamServiceImpl(MockExamRepository mockExamRepository, ExamRepository examRepository,
            StageRepository stageRepository, ExamTypeRepository examTypeRepository, MockExamMapper mapper) {
        this.mockExamRepository = mockExamRepository;
        this.examRepository = examRepository;
        this.stageRepository = stageRepository;
        this.examTypeRepository = examTypeRepository;
        this.mapper = mapper;
    }

    @Override
    public List<MockExamDto> findByStage(Long stageId) {
        if (!stageRepository.existsById(stageId)) {
            throw new ResourceNotFoundException(STAGE_NOT_FOUND);
        }
        List<MockExam> mockExams = mockExamRepository.findByStageId(stageId);
        Set<Long> withResults = withResults(mockExams.stream().flatMap(m -> m.getExams().stream()).toList());
        return mockExams.stream().sorted(ORDER).map(m -> mapper.toDto(m, withResults)).toList();
    }

    @Override
    public MockExamDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Override
    @Transactional
    public MockExamDto create(CreateMockExamRequest request) {
        Stage stage = stageRepository.findById(request.stageId())
                .orElseThrow(() -> new ResourceNotFoundException(STAGE_NOT_FOUND));
        // the same type sent twice is still one exam
        List<ExamType> types = new LinkedHashSet<>(request.examTypeIds()).stream().map(this::findExamType).toList();
        String name = request.name().trim();
        checkStage(stage, request.examDate());
        if (mockExamRepository.existsByStageIdAndNameIgnoreCase(stage.getId(), name)) {
            throw new BusinessRuleException(DUPLICATE_NAME);
        }

        MockExam mockExam = new MockExam();
        mockExam.setStage(stage);
        mockExam.setName(name);
        mockExam.setExamDate(request.examDate());
        MockExam saved = mockExamRepository.save(mockExam);
        types.forEach(type -> saved.getExams().add(newExam(saved, type)));
        log.info("Mock exam created: mockExamId={} stageId={} examTypeIds={}",
                saved.getId(), stage.getId(), types.stream().map(ExamType::getId).toList());
        return mapper.toDto(saved, Set.of());
    }

    @Override
    @Transactional
    public MockExamDto update(Long id, UpdateMockExamRequest request) {
        MockExam mockExam = getOrThrow(id);
        String name = request.name().trim();
        checkStage(mockExam.getStage(), request.examDate());
        if (mockExamRepository.existsByStageIdAndNameIgnoreCaseAndIdNot(mockExam.getStage().getId(), name, id)) {
            throw new BusinessRuleException(DUPLICATE_NAME);
        }
        mockExam.setName(name); // for every exam type at once
        mockExam.setExamDate(request.examDate());
        log.info("Mock exam updated: mockExamId={}", id);
        return toDto(mockExam);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MockExam mockExam = getOrThrow(id);
        if (!withResults(mockExam.getExams()).isEmpty()) {
            throw new BusinessRuleException(HAS_RESULTS);
        }
        // also in a closed cycle, as an empty stage: there is nothing to lose
        mockExam.getExams().forEach(exam -> exam.setDeleted(true));
        mockExam.setDeleted(true);
        log.info("Mock exam deleted: mockExamId={}", id);
    }

    @Override
    @Transactional
    public MockExamDto addExamType(Long id, AddExamTypeRequest request) {
        MockExam mockExam = getOrThrow(id);
        ExamType type = findExamType(request.examTypeId());
        checkOpenCycle(mockExam.getStage());
        if (examRepository.existsByMockExamIdAndExamTypeId(id, type.getId())) {
            throw new BusinessRuleException(TYPE_ALREADY_THERE.formatted(type.getName()));
        }
        Exam exam = newExam(mockExam, type);
        mockExam.getExams().add(exam);
        log.info("Exam type added: mockExamId={} examId={} examTypeId={}", id, exam.getId(), type.getId());
        return toDto(mockExam);
    }

    @Override
    @Transactional
    public MockExamDto removeExam(Long id, Long examId) {
        MockExam mockExam = getOrThrow(id);
        Exam exam = examRepository.findByIdAndMockExamId(examId, id)
                .orElseThrow(() -> new ResourceNotFoundException(EXAM_NOT_FOUND));
        checkOpenCycle(mockExam.getStage());
        if (!withResults(List.of(exam)).isEmpty()) {
            throw new BusinessRuleException(TYPE_HAS_RESULTS.formatted(exam.getExamType().getName()));
        }
        if (mockExam.getExams().size() == 1) {
            throw new BusinessRuleException(LAST_TYPE);
        }
        exam.setDeleted(true);
        mockExam.getExams().remove(exam);
        log.info("Exam type removed: mockExamId={} examId={}", id, examId);
        return toDto(mockExam);
    }

    private Exam newExam(MockExam mockExam, ExamType type) {
        Exam exam = new Exam();
        exam.setMockExam(mockExam);
        exam.setExamType(type);
        return examRepository.save(exam);
    }

    /** Mock exams of a closed cycle are history; the date always falls within the stage. */
    private static void checkStage(Stage stage, LocalDate examDate) {
        checkOpenCycle(stage);
        boolean beforeStart = examDate.isBefore(stage.getStartDate());
        boolean afterEnd = stage.getEndDate() != null && examDate.isAfter(stage.getEndDate());
        if (beforeStart || afterEnd) {
            throw new BusinessRuleException(OUTSIDE_STAGE);
        }
    }

    private static void checkOpenCycle(Stage stage) {
        if (stage.getCycle().getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException(CLOSED_CYCLE);
        }
    }

    private Set<Long> withResults(Collection<Exam> exams) {
        if (exams.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(examRepository.findIdsWithResults(exams.stream().map(Exam::getId).toList()));
    }

    private MockExamDto toDto(MockExam mockExam) {
        return mapper.toDto(mockExam, withResults(mockExam.getExams()));
    }

    private ExamType findExamType(Long id) {
        return examTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EXAM_TYPE_NOT_FOUND));
    }

    private MockExam getOrThrow(Long id) {
        return mockExamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MOCK_EXAM_NOT_FOUND));
    }
}
