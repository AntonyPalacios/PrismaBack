package pe.com.edu.prisma.students.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.com.edu.prisma.students.Enrollment;
import pe.com.edu.prisma.students.dto.EnrollmentDto;
import pe.com.edu.prisma.tutoring.mapper.TutorAssignmentMapper;
import pe.com.edu.prisma.users.User;

@Mapper(componentModel = "spring", imports = TutorAssignmentMapper.class)
public interface EnrollmentMapper {

    // the current tutor is not part of the enrollment row: the service looks it up (null = no tutor)
    @Mapping(target = "id", source = "enrollment.id")
    @Mapping(target = "studentId", source = "enrollment.student.id")
    @Mapping(target = "cycleId", source = "enrollment.stage.cycle.id")
    @Mapping(target = "cycleName", source = "enrollment.stage.cycle.name")
    @Mapping(target = "cycleStatus", source = "enrollment.stage.cycle.status")
    @Mapping(target = "stageId", source = "enrollment.stage.id")
    @Mapping(target = "stageName", source = "enrollment.stage.name")
    @Mapping(target = "stageStatus", source = "enrollment.stage.status")
    @Mapping(target = "careerId", source = "enrollment.career.id")
    @Mapping(target = "careerName", source = "enrollment.career.name")
    @Mapping(target = "active", source = "enrollment.active")
    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "tutorName", expression = "java(TutorAssignmentMapper.displayName(tutor))")
    EnrollmentDto toDto(Enrollment enrollment, User tutor);
}
