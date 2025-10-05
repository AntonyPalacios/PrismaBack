package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prismaapp.prisma.entities.StudentStageUser;
import pe.com.edu.prismaapp.prisma.repositories.StudentStageUserRepository;
import pe.com.edu.prismaapp.prisma.services.StudentStageUserService;

@Service
public class StudentStageUserImpl implements StudentStageUserService {

    private final StudentStageUserRepository studentStageUserRepository;

    public StudentStageUserImpl(StudentStageUserRepository studentStageUserRepository) {
        this.studentStageUserRepository = studentStageUserRepository;
    }

    @Override
    @Transactional
    public void deleteStudentStageUserByIdUser(Long idUser) {
        studentStageUserRepository.deleteByUser_Id(idUser);
    }

    @Override
    @Transactional
    public void save(StudentStageUser studentStageUser) {
        studentStageUserRepository.save(studentStageUser);
    }

    @Override
    public StudentStageUser findByStudentStageId(Long id) {
        return studentStageUserRepository.findByStudentStage_Id(id);
    }

    @Override
    @Transactional
    public void deleteStudent(Long id) {
        studentStageUserRepository.deleteByStudentStage_Student_Id(id);
    }

    @Override
    @Transactional
    public void deleteByStageId(Long stageId) {
        studentStageUserRepository.deleteByStudentStage_Stage_Id(stageId);
    }

    @Override
    public boolean isStudentAssignedToTutor(Long studentStageId, Long tutorId) {
        return studentStageUserRepository.existsByUser_IdAndStudentStage_Id(tutorId, studentStageId);
    }

    @Override
    @Transactional
    public void validateStudentTutor(Long studentStageId, Long tutorId) {
        StudentStageUser studentStageUser = studentStageUserRepository.findByStudentStage_Id(studentStageId);
        if (studentStageUser != null &&
                studentStageUser.getUser() != null &&
                !studentStageUser.getUser().getId().equals(tutorId)) {
            studentStageUserRepository.updateTutor(studentStageId,tutorId);
        }
    }
}
