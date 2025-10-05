package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prismaapp.prisma.dto.StudentDTO;
import pe.com.edu.prismaapp.prisma.entities.Stage;
import pe.com.edu.prismaapp.prisma.entities.Student;
import pe.com.edu.prismaapp.prisma.entities.StudentStage;
import pe.com.edu.prismaapp.prisma.repositories.StudentStageRepository;
import pe.com.edu.prismaapp.prisma.services.StudentStageService;
import pe.com.edu.prismaapp.prisma.services.StudentStageUserService;

@Service
public class StudentStageServiceImpl implements StudentStageService {

    private final StudentStageRepository studentStageRepository;
    private final StudentStageUserService studentStageUserService;

    public StudentStageServiceImpl(StudentStageRepository studentStageRepository,
                                   StudentStageUserService studentStageUserService) {
        this.studentStageRepository = studentStageRepository;
        this.studentStageUserService = studentStageUserService;
    }

    @Override
    public StudentStage save(StudentStage studentStage) {
        return studentStageRepository.save(studentStage);
    }

    @Override
    public StudentStage saveStudent(Student student, Stage stage, boolean active) {
        StudentStage studentStage = new StudentStage();
        studentStage.setStudent(student);
        studentStage.setStage(stage);
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

    @Override
    @Transactional
    public void deleteStudentStageByStageId(Long stageId) {
        //borrar studentStageUser
        studentStageUserService.deleteByStageId(stageId);
        studentStageRepository.deleteByStage_Id(stageId);
    }

    @Override
    @Transactional
    public void deleteStudent(Long id) {
        studentStageRepository.deleteByStudent_Id(id);
    }

    @Override
    public StudentStage getStudentStage(Long stageId, Long studentId) {
        return studentStageRepository.findByStudent_IdAndStage_Id(studentId, stageId);
    }

    @Override
    @Transactional
    public void validateStudentTutor(Long studentStage, Long tutorId) {
        studentStageUserService.validateStudentTutor(studentStage,tutorId);
    }
}
