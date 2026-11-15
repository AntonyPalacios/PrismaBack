package pe.com.edu.prisma.tutoring;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.edu.prisma.common.CreatedAuditEntity;
import pe.com.edu.prisma.students.Enrollment;
import pe.com.edu.prisma.users.User;

/**
 * Who tutors an enrollment, and since when (REQ-TUTORING-01). Append-only:
 * reassigning closes the current row ({@code validTo}) and opens a new one,
 * so "who was the tutor on date X" is always answerable. At most one open
 * row per enrollment (ux_tutor_assignment_current, V1). Closing is the only
 * change a row ever gets.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tutor_assignment")
public class TutorAssignment extends CreatedAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_stage_id", nullable = false, updatable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false, updatable = false)
    private User tutor;

    @Column(name = "valid_from", nullable = false, updatable = false)
    private OffsetDateTime validFrom;

    // null = this is the current tutor
    @Column(name = "valid_to")
    private OffsetDateTime validTo;
}
