package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "TEXAMRESULT")
public class ExamResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_exam_result")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_exam")
    private Exam exam;

    @ManyToOne
    @JoinColumn(name = "id_student_cycle")
    private StudentCycle studentCycle;

    @ManyToOne
    @JoinColumn(name = "id_course")
    private Course course;

    private int good;
    private int bad;
    private int blank;
    private double score;
}
