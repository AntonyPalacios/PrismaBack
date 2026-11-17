package pe.com.edu.prisma.students.dto;

import java.util.List;
import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;
import pe.com.edu.prisma.students.RelationshipType;

/**
 * A guardian of the student, with the relationship and the other students
 * they look after ("Apellidos, Nombres", by name): editing their contact
 * data changes it for those too.
 */
public record StudentParentDto(
        Long parentId,
        @Sensitive(LogMask.NAME) String fullName,
        @Sensitive(LogMask.HIDDEN) String phone,
        @Sensitive(LogMask.EMAIL) String email,
        RelationshipType relationshipType,
        List<String> otherStudents
) {
}
