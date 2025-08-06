package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "TEXAMRESULT")
@Data
@NoArgsConstructor
public class ExamResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_exam_result")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_exam", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_student_stage", nullable = false)
    private StudentStage studentStage;

    private int totalCorrect;
    private int totalIncorrect ;
    private Double totalScore;
    private int merit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area")
    private Area area;

    // Relación One-to-Many con ExamCourseResult
    @OneToMany(mappedBy = "examResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamCourseResult> courseResults;

    public ExamResult(Exam exam, StudentStage studentStage, Area area, int totalCorrect, int totalIncorrect, Double totalScore, int merit) {
        this.exam = exam;
        this.studentStage = studentStage;
        this.area = area;
        this.totalCorrect = totalCorrect;
        this.totalIncorrect = totalIncorrect;
        this.totalScore = totalScore;
        this.merit = merit;
    }
}
