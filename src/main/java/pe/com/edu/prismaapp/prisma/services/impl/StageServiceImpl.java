package pe.com.edu.prismaapp.prisma.services.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prismaapp.prisma.dto.StageDTO;
import pe.com.edu.prismaapp.prisma.entities.Cycle;
import pe.com.edu.prismaapp.prisma.entities.Stage;
import pe.com.edu.prismaapp.prisma.repositories.CycleRepository;
import pe.com.edu.prismaapp.prisma.repositories.StageRepository;
import pe.com.edu.prismaapp.prisma.services.StageService;
import pe.com.edu.prismaapp.prisma.util.UtilHelper;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StageServiceImpl implements StageService {

    private final StageRepository stageRepository;
    private final CycleRepository cycleRepository;

    public StageServiceImpl(StageRepository stageRepository, CycleRepository cycleRepository) {
        this.stageRepository = stageRepository;
        this.cycleRepository = cycleRepository;
    }

    @Override
    public StageDTO save(StageDTO stageDTO) {
        Stage stage = new Stage();
        stage.setName(stageDTO.getName());
        stage.setStartDate(stageDTO.getStartDate());
        stage.setEndDate(stageDTO.getEndDate());
        boolean isCurrent = UtilHelper.validateCurrent(stageDTO.getStartDate(),stageDTO.getEndDate());
        stage.setCurrent(isCurrent);
        Cycle cycle = cycleRepository.findById(stageDTO.getIdCycle())
                .orElseThrow(() -> new EntityNotFoundException("Ciclo no encontrado con ID: " + stageDTO.getIdCycle()));
        stage.setCycle(cycle);
        stage = stageRepository.save(stage);
        if(isCurrent){
            stageRepository.setCurrentFalseOthers(stage.getId());
        }
        stageDTO.setId(stage.getId());
        return stageDTO;
    }

    @Override
    public List<StageDTO> findAll(Long idCycle) {
        List<Stage> stages = stageRepository.findAllByCycle_IdOrderByStartDateDesc(idCycle);
        return stages.stream().map(StageDTO::new).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StageDTO update(Long id, StageDTO stageDTO) {
        Optional<Stage> optionalStage = stageRepository.findById(id);
        if (optionalStage.isPresent()) {
            Stage stage = optionalStage.get();
            stage.setName(stageDTO.getName());
            stage.setStartDate(stageDTO.getStartDate());
            stage.setEndDate(stageDTO.getEndDate());
            boolean isCurrent = UtilHelper.validateCurrent(stageDTO.getStartDate(),stageDTO.getEndDate());
            stage.setCurrent(isCurrent);
            stage = stageRepository.save(stage);
            if(isCurrent){
                stageRepository.setCurrentFalseOthers(stage.getId());
            }
            return stageDTO;
        }
        return stageDTO;
    }

    @Override
    public boolean delete(Long id) {
        Optional<Stage> optionalStage = stageRepository.findById(id);
        if (optionalStage.isPresent()) {
            Stage stage1 = optionalStage.get();
            stageRepository.delete(stage1);
        }
        return false;
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
