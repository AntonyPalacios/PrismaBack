package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.CycleDTO;

import java.util.List;

public interface CycleService {

    CycleDTO save(CycleDTO cycleDto);
    List<CycleDTO> findAll();

    CycleDTO update(Long id, CycleDTO cycle);

    boolean delete(Long id);
}
