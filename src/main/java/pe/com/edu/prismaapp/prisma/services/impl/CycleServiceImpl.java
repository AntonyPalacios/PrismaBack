package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prismaapp.prisma.dto.CycleApi;
import pe.com.edu.prismaapp.prisma.entities.Cycle;
import pe.com.edu.prismaapp.prisma.entities.Stage;
import pe.com.edu.prismaapp.prisma.errorHandler.ResourceNotFoundException;
import pe.com.edu.prismaapp.prisma.repositories.CycleRepository;
import pe.com.edu.prismaapp.prisma.services.CycleService;
import pe.com.edu.prismaapp.prisma.services.StageService;
import pe.com.edu.prismaapp.prisma.util.UtilHelper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CycleServiceImpl implements CycleService {

    private final CycleRepository cycleRepository;
    private final StageService stageService;

    public CycleServiceImpl(CycleRepository cycleRepository, StageService stageService) {
        this.cycleRepository = cycleRepository;
        this.stageService = stageService;
    }

    @Override
    public List<CycleApi.Response> findAll() {
        var cycles = cycleRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate"));
        return cycles.stream()
                .map(CycleApi.Response::from)
                .toList();
    }

    @Override
    @Transactional
    public CycleApi.Response save(CycleApi.Create cycleDTO) {
        var cycle = new Cycle();
        cycle.setName(cycleDTO.name());
        cycle.setStartDate(cycleDTO.startDate());
        cycle.setEndDate(cycleDTO.endDate());
        var isCurrent = UtilHelper.validateCurrent(cycleDTO.startDate(), cycleDTO.endDate());
        cycle.setCurrent(isCurrent);
        cycle = cycleRepository.saveAndFlush(cycle);
        if (isCurrent) {
            cycleRepository.setCurrentFalseOthers(cycle.getId());
        }
        return CycleApi.Response.from(cycle);
    }

    @Override
    @Transactional
    public CycleApi.Response update(Long id, CycleApi.Update cycleDTO) {
        var cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ciclo no encontrado con ID: " + id));
        cycle.setName(cycleDTO.name());
        cycle.setStartDate(cycleDTO.startDate());
        cycle.setEndDate(cycleDTO.endDate());
        var isCurrent = UtilHelper.validateCurrent(cycleDTO.startDate(), cycleDTO.endDate());
        cycle.setCurrent(isCurrent);
        cycle = cycleRepository.saveAndFlush(cycle);
        if (isCurrent) {
            cycleRepository.setCurrentFalseOthers(cycle.getId());
        }
        return CycleApi.Response.from(cycle);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var cycle = cycleRepository.findCycleByIdWithStages(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ciclo no encontrado con ID: " + id));

        stageService.deleteByCycleId(cycle.getId());
        cycleRepository.deleteById(id);
    }

    @Override
    public CycleApi.Response getCurrentCycle() {
        Cycle cycle = cycleRepository.findCycleByCurrentTrue();
        return CycleApi.Response.from(cycle);
    }
}
