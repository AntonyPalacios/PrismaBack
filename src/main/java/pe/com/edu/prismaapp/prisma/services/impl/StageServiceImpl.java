package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prismaapp.prisma.dto.StageApi;
import pe.com.edu.prismaapp.prisma.entities.Stage;
import pe.com.edu.prismaapp.prisma.errorHandler.ResourceNotFoundException;
import pe.com.edu.prismaapp.prisma.repositories.CycleRepository;
import pe.com.edu.prismaapp.prisma.repositories.StageRepository;
import pe.com.edu.prismaapp.prisma.services.StageService;
import pe.com.edu.prismaapp.prisma.services.StudentStageService;
import pe.com.edu.prismaapp.prisma.util.UtilHelper;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class StageServiceImpl implements StageService {

    private final StageRepository stageRepository;
    private final CycleRepository cycleRepository;
    private final StudentStageService studentStageService;

    public StageServiceImpl(StageRepository stageRepository, CycleRepository cycleRepository, StudentStageService studentStageService) {
        this.stageRepository = stageRepository;
        this.cycleRepository = cycleRepository;
        this.studentStageService = studentStageService;
    }

    @Override
    public List<StageApi.Response> findAll(Long idCycle) {
        List<Stage> stages = stageRepository.findAllByCycle_IdOrderByStartDateDesc(idCycle);
        return stages
                .stream()
                .map(StageApi.Response::from)
                .toList();
    }

    @Override
    @Transactional
    public StageApi.Response save(StageApi.Create stageDTO) {
        var stage = new Stage();
        stage.setName(stageDTO.name());
        stage.setStartDate(stageDTO.startDate());
        stage.setEndDate(stageDTO.endDate());
        var isCurrent = UtilHelper.validateCurrent(stageDTO.startDate(), stageDTO.endDate());
        stage.setCurrent(isCurrent);
        var cycle = cycleRepository.findById(stageDTO.idCycle())
                .orElseThrow(() -> new ResourceNotFoundException("Ciclo no encontrado con ID: " + stageDTO.idCycle()));
        stage.setCycle(cycle);
        stage = stageRepository.saveAndFlush(stage);
        if (isCurrent) {
            stageRepository.setCurrentFalseOthers(stage.getId());
        }

        return StageApi.Response.from(stage);
    }

    @Override
    @Transactional
    public StageApi.Response update(Long id, StageApi.Update stageDTO) {
        var stage = stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etapa no encontrada con ID: " + id));
        stage.setName(stageDTO.name());
        stage.setStartDate(stageDTO.startDate());
        stage.setEndDate(stageDTO.endDate());
        var isCurrent = UtilHelper.validateCurrent(stageDTO.startDate(), stageDTO.endDate());
        stage.setCurrent(isCurrent);
        var cycle = cycleRepository.findById(stageDTO.idCycle())
                .orElseThrow(() -> new ResourceNotFoundException("Ciclo no encontrado con ID: " + stageDTO.idCycle()));
        stage.setCycle(cycle);
        stage = stageRepository.saveAndFlush(stage);
        if (isCurrent) {
            stageRepository.setCurrentFalseOthers(stage.getId());
        }
        return StageApi.Response.from(stage);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etapa no encontrada con ID: " + id));
        //borrar studentStage
        studentStageService.deleteStudentStageByStageId(stage.getId());
        stageRepository.delete(stage);

    }

    @Override
    public Optional<Stage> getCurrentStage() {
        return stageRepository.findStageByCurrentTrue();
    }

    @Override
    public StageApi.Response getCurrentStageDTO() {
        var currentCycle = cycleRepository.findCycleByCurrentTrue().orElse(null);
        if(currentCycle == null) {
            throw new ResourceNotFoundException("Por favor, crear ciclos");
        }
        var currentStage = stageRepository.findStageByCurrentTrueAndCycle_Id(currentCycle.getId())
                .orElse(stageRepository.findAllByOrderByEndDateDesc().stream().findFirst().orElse(null));
        if(currentStage == null) {
            throw new ResourceNotFoundException("Por favor, crear etapas");
        }
        return StageApi.Response.from(currentStage);
    }

    @Override
    public Optional<Stage> getStageById(Long id) {
        return stageRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteByCycleId(Long cycleId) {
        stageRepository.deleteAllByCycle_Id(cycleId);
    }
}
