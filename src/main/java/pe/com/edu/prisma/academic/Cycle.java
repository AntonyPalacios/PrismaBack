package pe.com.edu.prisma.academic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import pe.com.edu.prisma.common.FullAuditEntity;

/**
 * The root unit of time of the academy: stages, exams and enrollments all
 * hang from a cycle. A CLOSED one is the archive.
 *
 * An admin can delete a cycle created by mistake. It is a soft delete, and
 * the restriction below hides deleted cycles from every query: for the rest
 * of the code a deleted cycle simply does not exist (the constraints of V8
 * ignore it too, so its name and dates are free again).
 */
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cycle")
public class Cycle extends FullAuditEntity {

    // unique ignoring case: enforced by ux_cycle_name_lower (V6) and checked in the service
    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // optional: a cycle without end date is open-ended
    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CycleStatus status = CycleStatus.PLANNED;

    // soft delete: see the @SQLRestriction on the class
    @Column(nullable = false)
    private boolean deleted = false;
}
