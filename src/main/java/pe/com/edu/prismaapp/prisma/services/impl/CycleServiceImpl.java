package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prismaapp.prisma.dto.CycleDTO;
import pe.com.edu.prismaapp.prisma.entities.Cycle;
import pe.com.edu.prismaapp.prisma.entities.Stage;
import pe.com.edu.prismaapp.prisma.repositories.CycleRepository;
import pe.com.edu.prismaapp.prisma.services.CycleService;
import pe.com.edu.prismaapp.prisma.services.StageService;
import pe.com.edu.prismaapp.prisma.util.UtilHelper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    public List<CycleDTO> findAll() {
        List<Cycle> cycles = cycleRepository.findAll();
        Collections.sort(cycles);

        return cycles.stream()
                .map(CycleDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CycleDTO save(CycleDTO cycleDTO) {
        Cycle cycle = new Cycle();
        mapValues(cycleDTO, cycle);
        cycleDTO.setId(cycle.getId());
        return cycleDTO;
    }

    @Override
    @Transactional
    public CycleDTO update(Long id, CycleDTO cycleDTO) {
        Cycle cycle = cycleRepository.findById(id).orElseThrow();
        mapValues(cycleDTO, cycle);
        return cycleDTO;
    }

    private void mapValues(CycleDTO cycleDTO, Cycle cycle) {
        cycle.setName(cycleDTO.getName());
        cycle.setStartDate(cycleDTO.getStartDate());
        cycle.setEndDate(cycleDTO.getEndDate());
        boolean isCurrent = UtilHelper.validateCurrent(cycleDTO.getStartDate(),cycleDTO.getEndDate());
        cycle.setCurrent(isCurrent);
        cycle = cycleRepository.save(cycle);
        if(isCurrent){
            cycleRepository.setCurrentFalseOthers(cycle.getId());
        }
        cycleDTO.setCurrent(isCurrent);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Cycle cycle = cycleRepository.findCycleByIdWithStages(id).orElseThrow();

        List<Stage> stages = cycle.getStages();
        List<Stage> stagesToDelete = new java.util.ArrayList<>(stages);

        for(Stage stage : stagesToDelete){
            stageService.delete(stage.getId());
        }

        cycleRepository.deleteById(id);
    }

    @Override
    public CycleDTO getCurrentCycle() {
        Cycle cycle = cycleRepository.findCycleByCurrentTrue();
        return new CycleDTO(cycle);
    }
}
