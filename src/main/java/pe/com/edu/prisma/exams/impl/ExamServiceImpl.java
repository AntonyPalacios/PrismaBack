package pe.com.edu.prisma.exams.impl;

import java.time.LocalDate;
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
import pe.com.edu.prisma.exams.ExamService;
import pe.com.edu.prisma.exams.dto.CreateExamsRequest;
import pe.com.edu.prisma.exams.dto.ExamDto;
import pe.com.edu.prisma.exams.dto.UpdateExamRequest;
import pe.com.edu.prisma.exams.mapper.ExamMapper;

@Service
@Transactional(readOnly = true)
public class ExamServiceImpl implements ExamService {

    static final String STAGE_NOT_FOUND = "No encontramos la etapa solicitada.";
    static final String EXAM_TYPE_NOT_FOUND = "No encontramos el tipo de examen solicitado.";
    static final String EXAM_NOT_FOUND = "No encontramos el examen solicitado.";
    static final String CLOSED_CYCLE = "Este ciclo está cerrado: sus exámenes ya no se pueden crear ni editar.";
    static final String OUTSIDE_STAGE = "La fecha del examen debe estar dentro de las fechas de su etapa.";
    static final String DUPLICATE = "Ya existe un examen %s con ese nombre en esta etapa.";
    static final String TYPE_LOCKED = "Este examen ya tiene resultados: su tipo de examen ya no se puede cambiar.";
    static final String HAS_RESULTS = "Este examen ya tiene resultados, así que no se puede eliminar.";

    private static final Logger log = LoggerFactory.getLogger(ExamServiceImpl.class);

    // newest first; the exams of one mock exam stay together, by type
    private static final Comparator<Exam> ORDER = Comparator.comparing(Exam::getExamDate).reversed()
            .thenComparing(exam -> exam.getName().toLowerCase())
            .thenComparing(exam -> exam.getExamType().getName())
            .thenComparing(Exam::getId);

    private final ExamRepository examRepository;
    private final StageRepository stageRepository;
    private final ExamTypeRepository examTypeRepository;
    private final ExamMapper examMapper;

    public ExamServiceImpl(ExamRepository examRepository, StageRepository stageRepository,
            ExamTypeRepository examTypeRepository, ExamMapper examMapper) {
        this.examRepository = examRepository;
        this.stageRepository = stageRepository;
        this.examTypeRepository = examTypeRepository;
        this.examMapper = examMapper;
    }

    @Override
    public List<ExamDto> findByStage(Long stageId) {
        if (!stageRepository.existsById(stageId)) {
            throw new ResourceNotFoundException(STAGE_NOT_FOUND);
        }
        List<Exam> exams = examRepository.findByStageId(stageId);
        Set<Long> withResults = exams.isEmpty() ? Set.of()
                : new HashSet<>(examRepository.findIdsWithResults(exams.stream().map(Exam::getId).toList()));
        return exams.stream()
                .sorted(ORDER)
                .map(exam -> examMapper.toDto(exam, withResults.contains(exam.getId())))
                .toList();
    }

    @Override
    public ExamDto findById(Long id) {
        Exam exam = getOrThrow(id);
        return examMapper.toDto(exam, hasResults(exam));
    }

    @Override
    @Transactional
    public List<ExamDto> create(CreateExamsRequest request) {
        Stage stage = stageRepository.findById(request.stageId())
                .orElseThrow(() -> new ResourceNotFoundException(STAGE_NOT_FOUND));
        // the same type sent twice is still one exam
        List<ExamType> types = new LinkedHashSet<>(request.examTypeIds()).stream().map(this::findExamType).toList();
        String name = request.name().trim();
        checkStage(stage, request.examDate());

        List<Exam> created = types.stream().map(type -> {
            if (examRepository.existsByStageIdAndExamTypeIdAndNameIgnoreCase(stage.getId(), type.getId(), name)) {
                throw new BusinessRuleException(DUPLICATE.formatted("de " + type.getName()));
            }
            Exam exam = new Exam();
            exam.setStage(stage);
            exam.setExamType(type);
            exam.setName(name);
            exam.setExamDate(request.examDate());
            return examRepository.save(exam);
        }).toList();
        log.info("Exams created: stageId={} examIds={}", stage.getId(), created.stream().map(Exam::getId).toList());
        return created.stream().map(exam -> examMapper.toDto(exam, false)).toList();
    }

    @Override
    @Transactional
    public ExamDto update(Long id, UpdateExamRequest request) {
        Exam exam = getOrThrow(id);
        ExamType type = findExamType(request.examTypeId());
        String name = request.name().trim();
        checkStage(exam.getStage(), request.examDate());
        boolean hasResults = hasResults(exam);
        if (hasResults && !type.getId().equals(exam.getExamType().getId())) {
            throw new BusinessRuleException(TYPE_LOCKED);
        }
        if (examRepository.existsByStageIdAndExamTypeIdAndNameIgnoreCaseAndIdNot(
                exam.getStage().getId(), type.getId(), name, id)) {
            throw new BusinessRuleException(DUPLICATE.formatted("de " + type.getName()));
        }
        exam.setName(name);
        exam.setExamDate(request.examDate());
        exam.setExamType(type);
        log.info("Exam updated: examId={} examTypeId={}", id, type.getId());
        return examMapper.toDto(exam, hasResults);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Exam exam = getOrThrow(id);
        if (hasResults(exam)) {
            throw new BusinessRuleException(HAS_RESULTS);
        }
        exam.setDeleted(true); // also in a closed cycle, as an empty stage: there is nothing to lose
        log.info("Exam deleted: examId={}", id);
    }

    /** Exams of a closed cycle are history; the date always falls within the stage. */
    private static void checkStage(Stage stage, LocalDate examDate) {
        if (stage.getCycle().getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException(CLOSED_CYCLE);
        }
        boolean beforeStart = examDate.isBefore(stage.getStartDate());
        boolean afterEnd = stage.getEndDate() != null && examDate.isAfter(stage.getEndDate());
        if (beforeStart || afterEnd) {
            throw new BusinessRuleException(OUTSIDE_STAGE);
        }
    }

    private boolean hasResults(Exam exam) {
        return examRepository.countResults(exam.getId()) > 0;
    }

    private ExamType findExamType(Long id) {
        return examTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EXAM_TYPE_NOT_FOUND));
    }

    private Exam getOrThrow(Long id) {
        return examRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(EXAM_NOT_FOUND));
    }
}
