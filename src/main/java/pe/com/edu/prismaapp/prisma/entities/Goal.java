package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "TGOAL")
@Data
public class Goal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_goal")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_exam")
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_student_stage", nullable = false)
    private StudentStage studentStage;

    private double scoreGoal;
    private int lectGoal;
    private int mateGoal;
}