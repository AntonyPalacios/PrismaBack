package pe.com.edu.prisma.students;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.catalog.Career;
import pe.com.edu.prisma.common.FullAuditEntity;

/**
 * A student's enrollment in one stage (REQ-STUDENTS-02): at most one per
 * student and stage (UNIQUE in V1). Each enrollment stands alone: a new one
 * never copies anything from an earlier stage. The career is optional and
 * can change mid-stage; exam results stay pinned to their exam's type, so
 * changing it never rewrites history. Withdrawing flips {@code active}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "student_stage")
public class Enrollment extends FullAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, updatable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false, updatable = false)
    private Stage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "career_id")
    private Career career;

    @Column(nullable = false)
    private boolean active = true;
}
