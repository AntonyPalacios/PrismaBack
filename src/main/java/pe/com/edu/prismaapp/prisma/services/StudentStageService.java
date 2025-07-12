package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.entities.Student;
import pe.com.edu.prismaapp.prisma.entities.StudentStage;

public interface StudentStageService {
    StudentStage save(StudentStage studentStage);

    StudentStage saveStudent(Student student);
}
