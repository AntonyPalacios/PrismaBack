package pe.com.edu.prisma.exams;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import pe.com.edu.prisma.catalog.ExamType;
import pe.com.edu.prisma.common.FullAuditEntity;

/**
 * One exam type of a mock exam (REQ-EXAMS-01): the results of the students
 * who sat that type hang from it, compared against its type. Its type never
 * changes: a wrong type is removed and the right one added. Name, date and
 * stage belong to the {@link MockExam}.
 *
 * Removing a type is a soft delete (only without results); the restriction
 * below hides removed exams from every query.
 */
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam")
public class Exam extends FullAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mock_exam_id", nullable = false, updatable = false)
    private MockExam mockExam;

    // at most once per mock exam: ux_exam_mock_exam_type (V11) and the service
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_type_id", nullable = false, updatable = false)
    private ExamType examType;

    @Column(nullable = false)
    private boolean deleted = false;
}
