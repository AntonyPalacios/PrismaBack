package pe.com.edu.prisma.students;

import java.util.List;
import pe.com.edu.prisma.students.dto.LinkParentRequest;
import pe.com.edu.prisma.students.dto.ParentDto;
import pe.com.edu.prisma.students.dto.StudentParentDto;
import pe.com.edu.prisma.students.dto.UpsertParentRequest;

/**
 * Guardians (REQ-STUDENTS-03). ADMIN and TUTOR read; only an ADMIN or the
 * student's current tutor change a student's guardians, as with the
 * student's own data.
 */
public interface ParentService {

    /** By name or phone digits, at most 20; fewer than 2 characters finds nobody. */
    List<ParentDto> search(String text);

    ParentDto create(UpsertParentRequest request);

    /** Contact data for every linked student: an ADMIN, or the current tutor of one of them. */
    ParentDto update(Long id, UpsertParentRequest request);

    /** The student's guardians, by name. */
    List<StudentParentDto> findByStudent(Long studentId);

    StudentParentDto link(Long studentId, LinkParentRequest request);

    void unlink(Long studentId, Long parentId);
}
