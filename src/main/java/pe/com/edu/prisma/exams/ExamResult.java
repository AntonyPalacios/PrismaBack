package pe.com.edu.prisma.exams;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.edu.prisma.common.FullAuditEntity;
import pe.com.edu.prisma.students.Enrollment;

/**
 * A student's result in one exam (REQ-EXAMS-02): hangs from their
 * enrollment in the exam's stage, at most once per exam (UNIQUE in V1).
 * Scores are stored as the academy's file brings them, never recalculated.
 * {@code attended = false} (0 correct and 0 wrong in every course) keeps the
 * row but leaves it out of averages and comparisons.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam_result")
public class ExamResult extends FullAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false, updatable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_stage_id", nullable = false, updatable = false)
    private Enrollment enrollment;

    @Column(name = "total_correct")
    private Integer totalCorrect;

    @Column(name = "total_wrong")
    private Integer totalWrong;

    @Column(name = "total_blank")
    private Integer totalBlank;

    @Column(name = "total_score", precision = 6, scale = 2)
    private BigDecimal totalScore;

    @Column(nullable = false)
    private boolean attended = true;

    @OneToMany(mappedBy = "examResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamCourseResult> courses = new ArrayList<>();

    @OneToMany(mappedBy = "examResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamAreaResult> areas = new ArrayList<>();
}
