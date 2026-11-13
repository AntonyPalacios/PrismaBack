package pe.com.edu.prisma.students;

import java.util.List;
import pe.com.edu.prisma.students.dto.StudentDto;
import pe.com.edu.prisma.students.dto.UpsertStudentRequest;

/**
 * Students' personal files: list, search, register and edit (REQ-STUDENTS-01);
 * retire and reactivate (REQ-STUDENTS-04).
 */
public interface StudentService {

    /**
     * Students by surnames, only the active ones unless {@code includeInactive};
     * {@code search} (optional) filters by name or DNI.
     */
    List<StudentDto> findAll(String search, boolean includeInactive);

    StudentDto findById(Long id);

    StudentDto create(UpsertStudentRequest request);

    /** Fails if the student is inactive: they are reactivated first. */
    StudentDto update(Long id, UpsertStudentRequest request);

    /** Soft-delete: only {@code active} changes; their history stays intact. */
    void deactivate(Long id);

    StudentDto reactivate(Long id);
}
