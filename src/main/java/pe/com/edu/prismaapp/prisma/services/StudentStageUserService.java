package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.entities.StudentStageUser;

public interface StudentStageUserService {
    void deleteStudentStageUserByIdUser(Long idUser);

    void save(StudentStageUser studentStageUser);

    StudentStageUser findByStudentStageId(Long id);

    void deleteStudent(Long id);

    void deleteByStageId(Long stageId);
}
