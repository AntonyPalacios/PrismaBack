package pe.com.edu.prisma.academic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import pe.com.edu.prisma.common.FullAuditEntity;

/**
 * A period of work inside a cycle: students enroll in a stage and tutors
 * are assigned per stage. Its dates always stay inside its cycle's dates
 * and never overlap with another stage of the same cycle.
 *
 * Deleting is a soft delete, and the restriction below hides deleted
 * stages from every query (the constraints of V9 ignore them too).
 */
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stage")
public class Stage extends FullAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private Cycle cycle;

    // unique within its cycle ignoring case: ux_stage_cycle_name_lower (V9) and the service
    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // optional and always set by the user, never by the system
    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StageStatus status = StageStatus.PLANNED;

    @Column(nullable = false)
    private boolean deleted = false;
}
