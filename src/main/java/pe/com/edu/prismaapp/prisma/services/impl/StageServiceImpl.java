package pe.com.edu.prismaapp.prisma.services.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prismaapp.prisma.dto.StageDTO;
import pe.com.edu.prismaapp.prisma.entities.Cycle;
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
import java.util.stream.Collectors;

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
    public List<StageDTO> findAll(Long idCycle) {
        List<Stage> stages = stageRepository.findAllByCycle_IdOrderByStartDateDesc(idCycle);
        return stages.stream().map(StageDTO::new).collect(Collectors.toList());
    }

    @Override
    public StageDTO save(StageDTO stageDTO) {
        Stage stage = new Stage();
        mapValues(stageDTO, stage);

        stageDTO.setId(stage.getId());
        return stageDTO;
    }

    @Override
    @Transactional
    public StageDTO update(Long id, StageDTO stageDTO) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etapa no encontrada con ID: " + id));
        mapValues(stageDTO, stage);
        return stageDTO;
    }

    private void mapValues(StageDTO stageDTO, Stage stage) {
        stage.setName(stageDTO.getName());
        stage.setStartDate(stageDTO.getStartDate());
        stage.setEndDate(stageDTO.getEndDate());
        boolean isCurrent = UtilHelper.validateCurrent(stageDTO.getStartDate(),stageDTO.getEndDate());
        stage.setCurrent(isCurrent);
        Cycle cycle = cycleRepository.findById(stageDTO.getIdCycle())
                .orElseThrow(() -> new ResourceNotFoundException("Ciclo no encontrado con ID: " + stageDTO.getIdCycle()));
        stage.setCycle(cycle);
        stage = stageRepository.save(stage);
        if(isCurrent){
            stageRepository.setCurrentFalseOthers(stage.getId());
        }
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
        Date currentDate = new Date();
        return stageRepository.findStageByStartDateBeforeAndEndDateAfter(currentDate,currentDate);
    }

    @Override
    public StageDTO getCurrentStageDTO() {
        Optional<Stage> optionalStage =  this.getCurrentStage();
        if(optionalStage.isPresent()){
            Stage stage = optionalStage.get();
            return new StageDTO(stage);
        }else{
            return null;
        }

    }
}
