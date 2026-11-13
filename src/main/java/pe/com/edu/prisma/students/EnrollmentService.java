package pe.com.edu.prisma.students;

import java.util.List;
import pe.com.edu.prisma.students.dto.CreateEnrollmentRequest;
import pe.com.edu.prisma.students.dto.EnrollmentDto;
import pe.com.edu.prisma.students.dto.UpdateEnrollmentRequest;

/** Enrolling students in stages (REQ-STUDENTS-02). */
public interface EnrollmentService {

    /** The student's enrollments, newest stage first. */
    List<EnrollmentDto> findByStudent(Long studentId);

    EnrollmentDto create(CreateEnrollmentRequest request);

    /** Sets or clears the career; only while the enrollment is active and its cycle is not closed. */
    EnrollmentDto update(Long id, UpdateEnrollmentRequest request);

    /** The student leaves the stage (soft: {@code active=false}); same conditions as {@link #update}. */
    void withdraw(Long id);
}
