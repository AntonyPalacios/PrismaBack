package pe.com.edu.prismaapp.prisma.services.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import pe.com.edu.prismaapp.prisma.dto.StageDTO;
import pe.com.edu.prismaapp.prisma.entities.Cycle;
import pe.com.edu.prismaapp.prisma.entities.Stage;
import pe.com.edu.prismaapp.prisma.repositories.CycleRepository;
import pe.com.edu.prismaapp.prisma.repositories.StageRepository;
import pe.com.edu.prismaapp.prisma.services.StageService;

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
        Cycle cycle = cycleRepository.findById(stageDTO.getIdCycle())
                .orElseThrow(() -> new EntityNotFoundException("Ciclo no encontrado con ID: " + stageDTO.getIdCycle()));
        stage.setCycle(cycle);
        stageRepository.save(stage);
        stageDTO.setId(stage.getId());
        return stageDTO;
    }

    @Override
    public List<StageDTO> findAll(Long idCycle) {
        List<Stage> stages = stageRepository.findAllByCycle_IdOrderByStartDateDesc(idCycle);
        return stages.stream().map(StageDTO::new).collect(Collectors.toList());
    }

    @Override
    public StageDTO update(Long id, StageDTO stageDTO) {
        Optional<Stage> optionalStage = stageRepository.findById(id);
        if (optionalStage.isPresent()) {
            Stage stage1 = optionalStage.get();
            stage1.setName(stageDTO.getName());
            stage1.setStartDate(stageDTO.getStartDate());
            stage1.setEndDate(stageDTO.getEndDate());
            stageRepository.save(stage1);
            return stageDTO;
        }
        return null;
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
}
