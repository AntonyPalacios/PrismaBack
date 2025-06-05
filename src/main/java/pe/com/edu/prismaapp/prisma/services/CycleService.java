package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.CycleDTO;
import pe.com.edu.prismaapp.prisma.entities.Cycle;

import java.util.List;

public interface CycleService {

    CycleDTO save(CycleDTO cycleDto);
    List<CycleDTO> findAll();
}
