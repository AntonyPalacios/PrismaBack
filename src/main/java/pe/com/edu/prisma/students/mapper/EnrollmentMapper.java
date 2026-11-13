package pe.com.edu.prisma.students.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.com.edu.prisma.students.Enrollment;
import pe.com.edu.prisma.students.dto.EnrollmentDto;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "cycleId", source = "stage.cycle.id")
    @Mapping(target = "cycleName", source = "stage.cycle.name")
    @Mapping(target = "cycleStatus", source = "stage.cycle.status")
    @Mapping(target = "stageId", source = "stage.id")
    @Mapping(target = "stageName", source = "stage.name")
    @Mapping(target = "stageStatus", source = "stage.status")
    @Mapping(target = "careerId", source = "career.id")
    @Mapping(target = "careerName", source = "career.name")
    EnrollmentDto toDto(Enrollment enrollment);
}
