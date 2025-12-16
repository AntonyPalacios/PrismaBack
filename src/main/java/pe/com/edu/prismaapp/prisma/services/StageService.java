package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.StageApi;
import pe.com.edu.prismaapp.prisma.entities.Stage;

import java.util.List;
import java.util.Optional;

public interface StageService {

    StageApi.Response save(StageApi.Create stageDTO);

    List<StageApi.Response> findAll(Long idCycle);

    StageApi.Response update(Long id, StageApi.Update stageDTO);

    void delete(Long id);

    Optional<Stage> getCurrentStage();

    StageApi.Response getCurrentStageDTO();

    Optional<Stage> getStageById(Long id);

    void deleteByCycleId(Long id);
}
