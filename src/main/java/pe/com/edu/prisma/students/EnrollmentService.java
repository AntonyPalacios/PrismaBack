package pe.com.edu.prisma.students;

import java.util.List;
import pe.com.edu.prisma.students.dto.BulkEnrollmentPreviewRequest;
import pe.com.edu.prisma.students.dto.BulkEnrollmentPreviewRow;
import pe.com.edu.prisma.students.dto.BulkEnrollmentRequest;
import pe.com.edu.prisma.students.dto.BulkEnrollmentResultDto;
import pe.com.edu.prisma.students.dto.CreateEnrollmentRequest;
import pe.com.edu.prisma.students.dto.EnrollmentDto;
import pe.com.edu.prisma.students.dto.UpdateEnrollmentRequest;

/** Enrolling students in stages (REQ-STUDENTS-02). */
public interface EnrollmentService {

    /** The student's enrollments, newest stage first. */
    List<EnrollmentDto> findByStudent(Long studentId);

    EnrollmentDto create(CreateEnrollmentRequest request);

    /**
     * Sets or clears the career and, when {@code tutorUserId} is given (ADMIN
     * only), reassigns the tutor — all or nothing. Only while the enrollment
     * is active and its cycle is not closed.
     */
    EnrollmentDto update(Long id, UpdateEnrollmentRequest request);

    /** The student leaves the stage (soft: {@code active=false}); same conditions as {@link #update}. */
    void withdraw(Long id);

    /**
     * Mass enrollment (REQ-STUDENTS-05): the students enrolled in the source
     * stage, by name, with the career and tutor they had there and whether the
     * batch will skip them. The target stage must be in a cycle not closed.
     */
    List<BulkEnrollmentPreviewRow> bulkPreview(BulkEnrollmentPreviewRequest request);

    /**
     * Enrolls every row in the target stage and assigns its tutor, all or
     * nothing: students already enrolled there or deactivated are skipped and
     * counted; any other problem (an inactive career, a user who is not an
     * active tutor) writes nothing.
     */
    BulkEnrollmentResultDto bulk(BulkEnrollmentRequest request);
}
