package pe.com.edu.prisma.tutoring.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.com.edu.prisma.tutoring.TutorAssignment;
import pe.com.edu.prisma.tutoring.dto.TutorAssignmentDto;
import pe.com.edu.prisma.users.User;

@Mapper(componentModel = "spring")
public interface TutorAssignmentMapper {

    @Mapping(target = "enrollmentId", source = "enrollment.id")
    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "tutorName", expression = "java(TutorAssignmentMapper.displayName(assignment.getTutor()))")
    TutorAssignmentDto toDto(TutorAssignment assignment);

    /** "Apellidos, Nombres", the agreed way to show a person; null without a tutor. */
    static String displayName(User user) {
        return user == null ? null : user.getLastNames() + ", " + user.getFirstName();
    }
}
