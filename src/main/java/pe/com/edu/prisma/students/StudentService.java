package pe.com.edu.prisma.students;

import java.util.List;
import pe.com.edu.prisma.students.dto.StudentDto;
import pe.com.edu.prisma.students.dto.UpsertStudentRequest;

/** Students' personal files (REQ-STUDENTS-01): list, search, register and edit. */
public interface StudentService {

    /** Active students by surnames; {@code search} (optional) filters by name or DNI. */
    List<StudentDto> findAll(String search);

    StudentDto findById(Long id);

    StudentDto create(UpsertStudentRequest request);

    StudentDto update(Long id, UpsertStudentRequest request);
}
