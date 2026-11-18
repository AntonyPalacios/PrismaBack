package pe.com.edu.prisma.goals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.edu.prisma.common.FullAuditEntity;
import pe.com.edu.prisma.exams.Exam;
import pe.com.edu.prisma.students.Enrollment;

/**
 * A student's goal of correct answers in one area for one exam (REQ-GOALS-01):
 * unique per enrollment, area and exam (V1). Always individual; it can aim
 * at a past exam too.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "goal")
public class Goal extends FullAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_stage_id", nullable = false, updatable = false)
    private Enrollment enrollment;

    @Column(name = "area_id", nullable = false, updatable = false)
    private Long areaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_exam_id", nullable = false, updatable = false)
    private Exam targetExam;

    @Column(name = "target_correct", nullable = false)
    private int targetCorrect;
}
