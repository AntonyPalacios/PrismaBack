package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.StudentApi;
import pe.com.edu.prismaapp.prisma.entities.Stage;
import pe.com.edu.prismaapp.prisma.entities.Student;
import pe.com.edu.prismaapp.prisma.entities.StudentStage;

public interface StudentStageService {
    StudentStage save(StudentStage studentStage);

    StudentStage saveStudent(Student student, Stage stage, boolean active);

    StudentStage updateStudent(StudentApi.Update student);

    void deleteStudentStageByStageId(Long stageId);

    void deleteStudent(Long id);

    StudentStage getStudentStage(Long stageId, Long studentId);

    void validateStudentTutor(Long id, Long tutorId);
}
