package pe.com.edu.prisma.exams;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.common.FullAuditEntity;

/**
 * A mock exam (simulacro): what the academy schedules in a stage, with a
 * name and a date. Each of its exam types is an {@link Exam}, which keeps
 * its own results; renaming or moving the date applies to all of them.
 *
 * Deleting is a soft delete (only while none of its exams has results); the
 * restriction below hides deleted mock exams from every query.
 */
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mock_exam")
public class MockExam extends FullAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false, updatable = false)
    private Stage stage;

    // unique within its stage ignoring case: ux_mock_exam_stage_name_lower (V11) and the service
    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(nullable = false)
    private boolean deleted = false;

    // its exam types; the removed ones are hidden by Exam's restriction
    @OneToMany(mappedBy = "mockExam")
    private List<Exam> exams = new ArrayList<>();
}
