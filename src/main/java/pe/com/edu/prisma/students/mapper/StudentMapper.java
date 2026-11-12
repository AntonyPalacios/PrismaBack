package pe.com.edu.prisma.students.mapper;

import org.mapstruct.Mapper;
import pe.com.edu.prisma.students.Student;
import pe.com.edu.prisma.students.dto.StudentDto;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    StudentDto toDto(Student student);
}
