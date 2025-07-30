package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TEXAMCOURSERESULT",uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_exam_result", "id_course"})
})
@Data
@NoArgsConstructor
public class ExamCourseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_exam_course_result")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_exam_result", nullable = false)
    private ExamResult examResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_course", nullable = false)
    private Course course;

    @Column(name = "course_score")
    private Double courseScore; // O Integer

    @Column(name = "course_correct")
    private Integer courseCorrect;

    @Column(name = "course_incorrect")
    private Integer courseIncorrect;

    @Column(name = "course_blank")
    private Integer courseBlank;

    public ExamCourseResult(ExamResult examResult, Course course, Integer courseCorrect, Integer courseIncorrect, Integer courseBlank) {
        this.examResult = examResult;
        this.course = course;
        this.courseCorrect = courseCorrect;
        this.courseIncorrect = courseIncorrect;
        this.courseBlank = courseBlank;
    }
}
