package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.StudentDTO;

import java.util.List;
import java.util.Optional;

public interface StudentService {

    StudentDTO save(StudentDTO studentDTO);
    List<StudentDTO> findAll(Long stageId, Optional<Long> userId);

    StudentDTO update(Long id, StudentDTO studentDTO);

    void delete(Long id);
}
