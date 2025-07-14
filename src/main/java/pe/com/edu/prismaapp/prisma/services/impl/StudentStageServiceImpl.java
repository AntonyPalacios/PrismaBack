package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import pe.com.edu.prismaapp.prisma.dto.StudentDTO;
import pe.com.edu.prismaapp.prisma.entities.Student;
import pe.com.edu.prismaapp.prisma.entities.StudentStage;
import pe.com.edu.prismaapp.prisma.repositories.StudentStageRepository;
import pe.com.edu.prismaapp.prisma.services.StageService;
import pe.com.edu.prismaapp.prisma.services.StudentStageService;

@Service
public class StudentStageServiceImpl implements StudentStageService {

    private final StudentStageRepository studentStageRepository;
    private final StageService stageService;

    public StudentStageServiceImpl(StudentStageRepository studentStageRepository, StageService stageService) {
        this.studentStageRepository = studentStageRepository;
        this.stageService = stageService;
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
