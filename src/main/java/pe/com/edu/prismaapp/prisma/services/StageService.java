package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.StageDTO;
import pe.com.edu.prismaapp.prisma.entities.Stage;

import java.util.List;
import java.util.Optional;

public interface StageService {

    StageDTO save(StageDTO stageDTO);

    List<StageDTO> findAll(Long idCycle);

    StageDTO update(Long id, StageDTO stageDTO);

    boolean delete(Long id);

    Optional<Stage> getCurrentStage();
}
