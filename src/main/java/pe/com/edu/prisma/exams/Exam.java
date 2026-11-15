package pe.com.edu.prisma.exams;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.catalog.ExamType;
import pe.com.edu.prisma.common.FullAuditEntity;

/**
 * One exam of one type in a stage (REQ-EXAMS-01). A mock exam taken by the
 * three tracks is three independent exams with the same name and date;
 * there is no entity grouping them. The type is fixed once the exam has
 * results, since every result is compared against its exam's type.
 *
 * Deleting is a soft delete (only without results); the restriction below
 * hides deleted exams from every query.
 */
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam")
public class Exam extends FullAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false, updatable = false)
    private Stage stage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_type_id", nullable = false)
    private ExamType examType;

    // unique within its stage and type ignoring case: ux_exam_stage_type_name_lower (V11) and the service
    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(nullable = false)
    private boolean deleted = false;
}
