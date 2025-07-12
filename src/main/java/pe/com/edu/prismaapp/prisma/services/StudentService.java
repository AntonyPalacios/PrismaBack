package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.StudentDTO;

import java.util.List;

public interface StudentService {

    StudentDTO save(StudentDTO studentDTO);
    List<StudentDTO> findAll(Long idStage);
}
