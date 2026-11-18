package pe.com.edu.prisma.students.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.com.edu.prisma.students.CurrentEnrollments.Current;
import pe.com.edu.prisma.students.Student;
import pe.com.edu.prisma.students.dto.StudentDto;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    // current = the student's current enrollment (CurrentEnrollments); null = none
    @Mapping(target = "id", source = "student.id")
    @Mapping(target = "firstName", source = "student.firstName")
    @Mapping(target = "lastNamePat", source = "student.lastNamePat")
    @Mapping(target = "lastNameMat", source = "student.lastNameMat")
    @Mapping(target = "dni", source = "student.dni")
    @Mapping(target = "email", source = "student.email")
    @Mapping(target = "phone", source = "student.phone")
    @Mapping(target = "birthDate", source = "student.birthDate")
    @Mapping(target = "active", source = "student.active")
    @Mapping(target = "currentStageName", source = "current.stageName")
    @Mapping(target = "tutorId", source = "current.tutorId")
    @Mapping(target = "tutorName", source = "current.tutorName")
    @Mapping(target = "careerName", source = "current.careerName")
    @Mapping(target = "examTypeId", source = "current.examTypeId")
    @Mapping(target = "examTypeName", source = "current.examTypeName")
    StudentDto toDto(Student student, Current current);
}
