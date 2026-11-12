package pe.com.edu.prisma.academic.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.academic.Cycle;
import pe.com.edu.prisma.academic.CycleRepository;
import pe.com.edu.prisma.academic.CycleService;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.academic.dto.ChangeCycleStatusRequest;
import pe.com.edu.prisma.academic.dto.CycleDto;
import pe.com.edu.prisma.academic.dto.UpsertCycleRequest;
import pe.com.edu.prisma.academic.mapper.CycleMapper;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ConfirmationRequiredException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class CycleServiceImpl implements CycleService {

    static final String DUPLICATE_NAME = "Ya existe un ciclo con ese nombre.";
    static final String OVERLAPPING_DATES = "Las fechas de este ciclo se cruzan con las de otro ciclo.";
    static final String CLOSED_NOT_EDITABLE = "Este ciclo está cerrado y ya no se puede editar.";
    static final String FORWARD_ONLY =
            "Un ciclo solo puede avanzar: de planificado a activo y de activo a cerrado.";
    static final String ANOTHER_ACTIVE = "Ya hay otro ciclo activo. Ciérralo antes de activar este.";
    static final String NOT_STARTED_ACTIVATE =
            "Este ciclo aún no empieza: su fecha de inicio es futura. Actívalo cuando llegue esa fecha "
                    + "o corrige la fecha de inicio.";
    static final String NOT_STARTED_CLOSE =
            "Este ciclo aún no empieza, así que no se puede cerrar. Corrige su fecha de inicio.";
    static final String END_DATE_REQUIRED_TO_CLOSE =
            "Este ciclo no tiene fecha de fin. Defínela antes de cerrarlo y elígela con cuidado: "
                    + "el siguiente ciclo solo podrá empezar después de esa fecha.";
    static final String CLOSING_EARLY =
            "Vas a cerrar el ciclo antes de su fecha de fin. La fecha de fin pasará a ser hoy "
                    + "y el siguiente ciclo solo podrá empezar después. Confirma para continuar.";

    private static final Logger log = LoggerFactory.getLogger(CycleServiceImpl.class);

    // a cycle without end date is open-ended: for the overlap check it runs "forever"
    private static final LocalDate OPEN_END = LocalDate.of(9999, 12, 31);

    // most recent first
    private static final Comparator<Cycle> ORDER =
            Comparator.comparing(Cycle::getStartDate).thenComparing(Cycle::getId).reversed();

    private final CycleRepository cycleRepository;
    private final CycleMapper cycleMapper;
    private final Clock clock; // "today" in Lima (ClockConfig)

    public CycleServiceImpl(CycleRepository cycleRepository, CycleMapper cycleMapper, Clock clock) {
        this.cycleRepository = cycleRepository;
        this.cycleMapper = cycleMapper;
        this.clock = clock;
    }

    @Override
    public List<CycleDto> findAll() {
        return cycleRepository.findAll().stream().sorted(ORDER).map(cycleMapper::toDto).toList();
    }

    @Override
    public CycleDto findById(Long id) {
        return cycleMapper.toDto(getOrThrow(id));
    }

    @Override
    @Transactional
    public CycleDto create(UpsertCycleRequest request) {
        String name = request.name().trim();
        if (cycleRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException(DUPLICATE_NAME);
        }
        rejectOverlap(request, null);

        Cycle cycle = new Cycle(); // born PLANNED
        apply(cycle, name, request);
        Cycle saved = cycleRepository.save(cycle);
        log.info("Cycle created: cycleId={} startDate={} endDate={}",
                saved.getId(), saved.getStartDate(), saved.getEndDate());
        return cycleMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CycleDto update(Long id, UpsertCycleRequest request) {
        Cycle cycle = getOrThrow(id);
        if (cycle.getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException(CLOSED_NOT_EDITABLE);
        }
        String name = request.name().trim();
        if (cycleRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessRuleException(DUPLICATE_NAME);
        }
        rejectOverlap(request, id);

        apply(cycle, name, request);
        log.info("Cycle updated: cycleId={} startDate={} endDate={}", id, cycle.getStartDate(), cycle.getEndDate());
        return cycleMapper.toDto(cycle);
    }

    @Override
    @Transactional
    public CycleDto changeStatus(Long id, ChangeCycleStatusRequest request) {
        Cycle cycle = getOrThrow(id);
        CycleStatus from = cycle.getStatus();
        CycleStatus to = request.status();
        LocalDate today = LocalDate.now(clock);

        if (from == CycleStatus.PLANNED && to == CycleStatus.ACTIVE) {
            activate(cycle, today);
        } else if (from == CycleStatus.ACTIVE && to == CycleStatus.CLOSED) {
            close(cycle, today, request.confirmed());
        } else {
            throw new BusinessRuleException(FORWARD_ONLY);
        }
        log.info("Cycle status changed: cycleId={} from={} to={} endDate={}", id, from, to, cycle.getEndDate());
        return cycleMapper.toDto(cycle);
    }

    private void activate(Cycle cycle, LocalDate today) {
        if (cycle.getStartDate().isAfter(today)) {
            throw new BusinessRuleException(NOT_STARTED_ACTIVATE);
        }
        if (cycleRepository.existsByStatus(CycleStatus.ACTIVE)) {
            throw new BusinessRuleException(ANOTHER_ACTIVE);
        }
        cycle.setStatus(CycleStatus.ACTIVE);
    }

    // a closed cycle always ends up with an end date that is not in the future
    private void close(Cycle cycle, LocalDate today, boolean confirmed) {
        if (cycle.getStartDate().isAfter(today)) {
            throw new BusinessRuleException(NOT_STARTED_CLOSE);
        }
        if (cycle.getEndDate() == null) {
            throw new BusinessRuleException(END_DATE_REQUIRED_TO_CLOSE);
        }
        if (cycle.getEndDate().isAfter(today)) {
            if (!confirmed) {
                throw new ConfirmationRequiredException(CLOSING_EARLY);
            }
            cycle.setEndDate(today); // closing early: the cycle ends today
        }
        cycle.setStatus(CycleStatus.CLOSED);
    }

    private void rejectOverlap(UpsertCycleRequest request, Long excludedId) {
        LocalDate end = request.endDate() != null ? request.endDate() : OPEN_END;
        if (cycleRepository.existsOverlapping(request.startDate(), end, excludedId)) {
            throw new BusinessRuleException(OVERLAPPING_DATES);
        }
    }

    private static void apply(Cycle cycle, String name, UpsertCycleRequest request) {
        cycle.setName(name);
        cycle.setStartDate(request.startDate());
        cycle.setEndDate(request.endDate());
    }

    private Cycle getOrThrow(Long id) {
        return cycleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el ciclo solicitado."));
    }
}
