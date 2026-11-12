package pe.com.edu.prisma.academic.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.academic.Cycle;
import pe.com.edu.prisma.academic.CycleRepository;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.academic.StageRepository;
import pe.com.edu.prisma.academic.StageService;
import pe.com.edu.prisma.academic.StageStatus;
import pe.com.edu.prisma.academic.dto.StageDto;
import pe.com.edu.prisma.academic.dto.UpsertStageRequest;
import pe.com.edu.prisma.academic.mapper.StageMapper;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.common.logging.LogSanitizer;

@Service
@Transactional(readOnly = true)
public class StageServiceImpl implements StageService {

    static final String CYCLE_CLOSED = "Este ciclo está cerrado: ya no se pueden crear ni editar sus etapas.";
    static final String STAGE_CLOSED = "Esta etapa está cerrada y ya no se puede editar.";
    static final String OUTSIDE_CYCLE =
            "Las fechas de la etapa deben estar dentro de las fechas de su ciclo: "
                    + "no puede empezar antes ni terminar después que el ciclo.";
    static final String DUPLICATE_NAME = "Ya existe una etapa con ese nombre en este ciclo.";
    static final String OVERLAPPING_DATES = "Las fechas de esta etapa se cruzan con las de otra etapa del ciclo.";
    static final String HAS_DATA =
            "Esta etapa tiene alumnos matriculados o exámenes, así que no se puede eliminar.";

    private static final Logger log = LoggerFactory.getLogger(StageServiceImpl.class);

    // a stage without end date is open-ended: for the overlap check it runs "forever"
    private static final LocalDate OPEN_END = LocalDate.of(9999, 12, 31);

    private final StageRepository stageRepository;
    private final CycleRepository cycleRepository;
    private final StageMapper stageMapper;
    private final Clock clock; // "today" in Lima (ClockConfig)

    public StageServiceImpl(StageRepository stageRepository, CycleRepository cycleRepository,
            StageMapper stageMapper, Clock clock) {
        this.stageRepository = stageRepository;
        this.cycleRepository = cycleRepository;
        this.stageMapper = stageMapper;
        this.clock = clock;
    }

    @Override
    public List<StageDto> findAll(Long cycleId) {
        getCycleOrThrow(cycleId);
        List<Stage> stages = stageRepository.findByCycleIdOrderByStartDateAscIdAsc(cycleId);
        Long currentId = currentStageId(stages);
        return stages.stream().map(stage -> stageMapper.toDto(stage, stage.getId().equals(currentId))).toList();
    }

    @Override
    public StageDto findById(Long cycleId, Long id) {
        return toDto(getOrThrow(cycleId, id));
    }

    @Override
    @Transactional
    public StageDto create(Long cycleId, UpsertStageRequest request) {
        Cycle cycle = getCycleOrThrow(cycleId);
        if (cycle.getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException(CYCLE_CLOSED);
        }
        String name = request.name().trim();
        rejectOutsideCycle(cycle, request);
        if (stageRepository.existsByCycleIdAndNameIgnoreCase(cycleId, name)) {
            throw new BusinessRuleException(DUPLICATE_NAME);
        }
        rejectOverlap(cycleId, request, null);

        Stage stage = new Stage(); // born PLANNED
        stage.setCycle(cycle);
        apply(stage, name, request);
        Stage saved = stageRepository.save(stage);
        log.info("Stage created: stageId={} cycleId={} startDate={} endDate={}",
                saved.getId(), cycleId, saved.getStartDate(), saved.getEndDate());
        return toDto(saved);
    }

    @Override
    @Transactional
    public StageDto update(Long cycleId, Long id, UpsertStageRequest request) {
        Stage stage = getOrThrow(cycleId, id);
        Cycle cycle = stage.getCycle();
        if (cycle.getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException(CYCLE_CLOSED);
        }
        if (stage.getStatus() == StageStatus.CLOSED) {
            throw new BusinessRuleException(STAGE_CLOSED);
        }
        String name = request.name().trim();
        rejectOutsideCycle(cycle, request);
        if (stageRepository.existsByCycleIdAndNameIgnoreCaseAndIdNot(cycleId, name, id)) {
            throw new BusinessRuleException(DUPLICATE_NAME);
        }
        rejectOverlap(cycleId, request, id);

        apply(stage, name, request);
        log.info("Stage updated: stageId={} cycleId={} startDate={} endDate={}",
                id, cycleId, stage.getStartDate(), stage.getEndDate());
        return toDto(stage);
    }

    // allowed in any status and also in a closed cycle: the only condition is "no data"
    @Override
    @Transactional
    public void delete(Long cycleId, Long id) {
        Stage stage = getOrThrow(cycleId, id);
        if (stageRepository.countEnrollments(id) > 0 || stageRepository.countExams(id) > 0) {
            throw new BusinessRuleException(HAS_DATA);
        }
        stage.setDeleted(true); // soft delete: from here on no query sees it
        // the log line is the only trace of a deletion (a stage name is not personal data)
        log.info("Stage deleted: stageId={} cycleId={} name={} status={} startDate={} endDate={}", id, cycleId,
                LogSanitizer.sanitize(stage.getName()), stage.getStatus(), stage.getStartDate(), stage.getEndDate());
    }

    /**
     * The stage in force: the one containing today; if none does, the one
     * that started most recently — whatever its status or the cycle's.
     * Stages never overlap, so both cases are the same stage: the latest one
     * that has already started. None if no stage has started yet.
     */
    private Long currentStageId(List<Stage> stagesByStartDate) {
        LocalDate today = LocalDate.now(clock);
        Long currentId = null;
        for (Stage stage : stagesByStartDate) {
            if (!stage.getStartDate().isAfter(today)) {
                currentId = stage.getId();
            }
        }
        return currentId;
    }

    private StageDto toDto(Stage stage) {
        Long cycleId = stage.getCycle().getId();
        Long currentId = currentStageId(stageRepository.findByCycleIdOrderByStartDateAscIdAsc(cycleId));
        return stageMapper.toDto(stage, stage.getId().equals(currentId));
    }

    // a stage never starts before its cycle nor ends after it
    private static void rejectOutsideCycle(Cycle cycle, UpsertStageRequest request) {
        boolean startsBefore = request.startDate().isBefore(cycle.getStartDate());
        boolean endsAfter = cycle.getEndDate() != null
                && (request.startDate().isAfter(cycle.getEndDate())
                        || (request.endDate() != null && request.endDate().isAfter(cycle.getEndDate())));
        if (startsBefore || endsAfter) {
            throw new BusinessRuleException(OUTSIDE_CYCLE);
        }
    }

    private void rejectOverlap(Long cycleId, UpsertStageRequest request, Long excludedId) {
        LocalDate end = request.endDate() != null ? request.endDate() : OPEN_END;
        if (stageRepository.existsOverlapping(cycleId, request.startDate(), end, excludedId)) {
            throw new BusinessRuleException(OVERLAPPING_DATES);
        }
    }

    private static void apply(Stage stage, String name, UpsertStageRequest request) {
        stage.setName(name);
        stage.setStartDate(request.startDate());
        stage.setEndDate(request.endDate());
    }

    private Cycle getCycleOrThrow(Long cycleId) {
        return cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el ciclo solicitado."));
    }

    // a deleted cycle hides its stages too: the cycle must exist first
    private Stage getOrThrow(Long cycleId, Long id) {
        getCycleOrThrow(cycleId);
        return stageRepository.findByIdAndCycleId(id, cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos la etapa solicitada."));
    }
}
