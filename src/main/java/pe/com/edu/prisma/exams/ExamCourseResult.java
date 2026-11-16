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
import pe.com.edu.prisma.catalog.Course;
import pe.com.edu.prisma.common.BaseEntity;

/** Correct, wrong and blank answers of one course in a result. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam_course_result")
public class ExamCourseResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_result_id", nullable = false, updatable = false)
    private ExamResult examResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    private Course course;

    @Column(nullable = false)
    private int correct;

    @Column(nullable = false)
    private int wrong;

    @Column(nullable = false)
    private int blank;
}
