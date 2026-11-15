package pe.com.edu.prisma.students;

import java.util.List;
import pe.com.edu.prisma.students.dto.StudentDto;
import pe.com.edu.prisma.students.dto.StudentFilter;
import pe.com.edu.prisma.students.dto.UpsertStudentRequest;

/**
 * Students' personal files: list, search, register and edit (REQ-STUDENTS-01);
 * retire and reactivate (REQ-STUDENTS-04).
 */
public interface StudentService {

    /**
     * Students by surnames, only the active ones unless the filter includes
     * inactive ones, searched by name or DNI. Only ADMIN filters by tutor; a
     * TUTOR always gets just the students assigned to them today.
     */
    List<StudentDto> findAll(StudentFilter filter);

    StudentDto findById(Long id);

    StudentDto create(UpsertStudentRequest request);

    /** Only ADMIN or the student's current tutor; fails if the student is inactive (reactivated first). */
    StudentDto update(Long id, UpsertStudentRequest request);

    /** Soft-delete: only {@code active} changes; their history stays intact. */
    void deactivate(Long id);

    StudentDto reactivate(Long id);
}
