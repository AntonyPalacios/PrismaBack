package pe.com.edu.prisma.academic.impl;

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
import pe.com.edu.prisma.academic.dto.CycleDto;
import pe.com.edu.prisma.academic.dto.UpsertCycleRequest;
import pe.com.edu.prisma.academic.mapper.CycleMapper;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class CycleServiceImpl implements CycleService {

    static final String DUPLICATE_NAME = "Ya existe un ciclo con ese nombre.";
    static final String OVERLAPPING_DATES = "Las fechas de este ciclo se cruzan con las de otro ciclo.";
    static final String CLOSED_NOT_EDITABLE = "Este ciclo está cerrado y ya no se puede editar.";

    private static final Logger log = LoggerFactory.getLogger(CycleServiceImpl.class);

    // a cycle without end date is open-ended: for the overlap check it runs "forever"
    private static final LocalDate OPEN_END = LocalDate.of(9999, 12, 31);

    // most recent first
    private static final Comparator<Cycle> ORDER =
            Comparator.comparing(Cycle::getStartDate).thenComparing(Cycle::getId).reversed();

    private final CycleRepository cycleRepository;
    private final CycleMapper cycleMapper;

    public CycleServiceImpl(CycleRepository cycleRepository, CycleMapper cycleMapper) {
        this.cycleRepository = cycleRepository;
        this.cycleMapper = cycleMapper;
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
