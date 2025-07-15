package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import pe.com.edu.prismaapp.prisma.dto.StudentDTO;
import pe.com.edu.prismaapp.prisma.entities.Stage;
import pe.com.edu.prismaapp.prisma.entities.Student;
import pe.com.edu.prismaapp.prisma.entities.StudentStage;
import pe.com.edu.prismaapp.prisma.repositories.StageRepository;
import pe.com.edu.prismaapp.prisma.repositories.StudentStageRepository;
import pe.com.edu.prismaapp.prisma.services.StageService;
import pe.com.edu.prismaapp.prisma.services.StudentStageService;

import java.util.Optional;

@Service
public class StudentStageServiceImpl implements StudentStageService {

    private final StudentStageRepository studentStageRepository;
    private final StageService stageService;
    private final StageRepository stageRepository;

    public StudentStageServiceImpl(StudentStageRepository studentStageRepository, StageService stageService, StageRepository stageRepository) {
        this.studentStageRepository = studentStageRepository;
        this.stageService = stageService;
        this.stageRepository = stageRepository;
    }

    @Override
    public StudentStage save(StudentStage studentStage) {
        return studentStageRepository.save(studentStage);
    }

    @Override
    public StudentStage saveStudent(Student student, boolean active) {
        StudentStage studentStage = new StudentStage();
        studentStage.setStudent(student);
        stageService.getCurrentStage().ifPresent(studentStage::setStage);
        studentStage.setActive(active);
        return studentStageRepository.save(studentStage);
    }

    @Override
    public StudentStage updateStudent(Student student, StudentDTO studentDTO) {
        StudentStage studentStage = studentStageRepository.findByStudent_IdAndStage_Id(student.getId(), studentDTO.getStageId());
        studentStage.setActive(studentDTO.isActive());
        studentStageRepository.save(studentStage);
        return studentStage;
    }
}
