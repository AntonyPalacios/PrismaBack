package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "TSTUDENTSTAGEUSER")
@Data
public class StudentStageUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_studentstageuser")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_user")
    private User user;

    @ManyToOne
    @JoinColumn(name = "id_student_stage")
    private StudentStage studentStage;
}
