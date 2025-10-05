package pe.com.edu.prismaapp.prisma.services;

import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.StudentDTO;
import pe.com.edu.prismaapp.prisma.entities.Student;

import java.util.List;
import java.util.Optional;

public interface StudentService {

    StudentDTO save(StudentDTO studentDTO);
    List<StudentDTO> findAll(Optional<Long> stageId);

    StudentDTO update(Long id, StudentDTO studentDTO);

    void delete(Long id);

    boolean isStudentAssignedToTutor(Long studentId, Long tutorId);

    void uploadStudents(MultipartFile file);

    Student findByDniOrName(String dni,String name);

    Student findById(Long id);
}
