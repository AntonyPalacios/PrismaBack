package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import pe.com.edu.prismaapp.prisma.dto.CycleDTO;
import pe.com.edu.prismaapp.prisma.entities.Cycle;
import pe.com.edu.prismaapp.prisma.repositories.CycleRepository;
import pe.com.edu.prismaapp.prisma.services.CycleService;
import pe.com.edu.prismaapp.prisma.util.UtilHelper;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CycleServiceImpl implements CycleService {

    private final CycleRepository cycleRepository;

    public CycleServiceImpl(CycleRepository cycleRepository) {
        this.cycleRepository = cycleRepository;
    }

    @Override
    public CycleDTO save(CycleDTO cycleDTO) {
        Cycle cycle = new Cycle();
        cycle.setName(cycleDTO.getName());
        cycle.setStartDate(cycleDTO.getStartDate());
        cycle.setEndDate(cycleDTO.getEndDate());
        boolean isCurrent = UtilHelper.validateCurrent(cycleDTO.getStartDate(),cycleDTO.getEndDate());
        cycle.setCurrent(isCurrent);
        cycle = cycleRepository.save(cycle);
        if(isCurrent){
            cycleRepository.setCurrentFalseOthers(cycle.getId());
        }
        cycleDTO.setId(cycle.getId());
        cycleDTO.setCurrent(isCurrent);
        return cycleDTO;
    }

    @Override
    public List<CycleDTO> findAll() {
        List<Cycle> cycles = cycleRepository.findAll();

        return cycles.stream()
                .map(CycleDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public CycleDTO update(Long id, CycleDTO cycleDTO) {
        Cycle cycle = cycleRepository.findById(id).orElseThrow();
        cycle.setName(cycleDTO.getName());
        cycle.setStartDate(cycleDTO.getStartDate());
        cycle.setEndDate(cycleDTO.getEndDate());
        boolean isCurrent = UtilHelper.validateCurrent(cycleDTO.getStartDate(),cycleDTO.getEndDate());
        cycle.setCurrent(isCurrent);
        cycleRepository.save(cycle);
        if(isCurrent){
            cycleRepository.setCurrentFalseOthers(cycle.getId());
        }
        cycleDTO.setCurrent(isCurrent);
        return cycleDTO;
    }

    @Override
    public boolean delete(Long id) {
        Cycle cycle = cycleRepository.findById(id).orElseThrow();
        cycleRepository.delete(cycle);
        return true;
    }

    @Override
    public CycleDTO getCurrentCycle() {
        Cycle cycle = cycleRepository.findCycleByCurrentTrue();
        return new CycleDTO(cycle);
    }
}
