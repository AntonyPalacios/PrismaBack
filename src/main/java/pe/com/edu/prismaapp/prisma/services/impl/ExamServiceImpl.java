package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prismaapp.prisma.dto.ExamDTO;
import pe.com.edu.prismaapp.prisma.entities.Exam;
import pe.com.edu.prismaapp.prisma.entities.Stage;
import pe.com.edu.prismaapp.prisma.errorHandler.ResourceNotFoundException;
import pe.com.edu.prismaapp.prisma.repositories.ExamRepository;
import pe.com.edu.prismaapp.prisma.services.ExamService;
import pe.com.edu.prismaapp.prisma.services.StageService;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final StageService stageService;

    public ExamServiceImpl(ExamRepository examRepository, StageService stageService) {
        this.examRepository = examRepository;
        this.stageService = stageService;
    }

    @Override
    public List<ExamDTO> getExams(Long cycleId, Long stageId) {
        List<Exam> exams = new ArrayList<>();
        List<ExamDTO> examsDto = new ArrayList<>();
        if (cycleId == null && stageId == null) {
            return examsDto;
        }

        //se busca por etapa
        if(cycleId == null){
            exams = examRepository.findAllByStage_IdOrderByDateAsc(stageId);
        }

        //se listan todos los exams del ciclo
        if(stageId == null){
            exams = examRepository.findAllByStage_Cycle_IdOrderByDateAsc(cycleId);
        }

        for (Exam exam : exams) {
            ExamDTO examDto = new ExamDTO();
            examDto.setId(exam.getId());
            examDto.setDate(exam.getDate());
            examDto.setName(exam.getName());
            examDto.setStage(exam.getStage().getName());
            examDto.setStageId(exam.getStage().getId());
            examDto.setCycle(exam.getStage().getCycle().getName());
            examsDto.add(examDto);
        }

        return examsDto;
    }

    @Override
    @Transactional
    public ExamDTO save(ExamDTO examDTO) {
        Exam exam = new Exam();
        exam.setName(examDTO.getName());
        exam.setDate(examDTO.getDate());
        if(examDTO.getStageId() != null){
            Stage stage = stageService.getStageById(examDTO.getStageId())
                    .orElseThrow(()->new ResourceNotFoundException("Etapa no encontrada con ID: " + examDTO.getStageId()));
            exam.setStage(stage);
        }else{
            throw new RuntimeException("Seleccione etapa");
        }
        examRepository.save(exam);
        examDTO.setId(exam.getId());
        return examDTO;
    }

    @Override
    @Transactional
    public ExamDTO update(Long id, ExamDTO examDTO) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Etapa no encontrada con ID: " + examDTO.getStageId()));
        exam.setName(examDTO.getName());
        exam.setDate(examDTO.getDate());
        examRepository.save(exam);
        return examDTO;
    }
}
