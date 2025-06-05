package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import pe.com.edu.prismaapp.prisma.dto.CycleDTO;
import pe.com.edu.prismaapp.prisma.entities.Cycle;
import pe.com.edu.prismaapp.prisma.repositories.CycleRepository;
import pe.com.edu.prismaapp.prisma.services.CycleService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CycleServiceImpl implements CycleService {

    private CycleRepository cycleRepository;

    public CycleServiceImpl(CycleRepository cycleRepository) {
        this.cycleRepository = cycleRepository;
    }

    @Override
    public CycleDTO save(CycleDTO cycleDTO) {
        Cycle cycle = new Cycle();
        cycle.setName(cycleDTO.getName());
        cycle.setStartDate(cycleDTO.getStartDate());
        cycle.setEndDate(cycleDTO.getEndDate());
        cycle = cycleRepository.save(cycle);
        cycleDTO.setId(cycle.getId());
        return cycleDTO;
    }

    @Override
    public List<CycleDTO> findAll() {
        List<Cycle> cycles = cycleRepository.findAll();

        return cycles.stream()
                .map(CycleDTO::new)
                .collect(Collectors.toList());
    }
}
