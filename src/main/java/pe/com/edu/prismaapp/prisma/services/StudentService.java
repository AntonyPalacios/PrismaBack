package pe.com.edu.prismaapp.prisma.services;

import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.StudentApi;
import pe.com.edu.prismaapp.prisma.entities.Student;

import java.util.List;
import java.util.Optional;

public interface StudentService {

    StudentApi.Response save(StudentApi.Create studentDTO);

    List<StudentApi.Response> findAll(Optional<Long> stageId);

    StudentApi.Response update(Long id, StudentApi.Update studentDTO);

    void delete(Long id);

    boolean isStudentAssignedToTutor(Long studentId, Long tutorId);

    void uploadStudents(MultipartFile file);

    Student findByDniOrName(String dni,String name);

    Student findById(Long id);
}
