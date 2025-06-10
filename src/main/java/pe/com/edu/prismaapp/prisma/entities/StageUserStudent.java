package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "TSTAGEUSERSTUDENT")
@Data
public class StageUserStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_stageuserstudent")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_stage")
    private Stage stage;

    @ManyToOne
    @JoinColumn(name = "id_user_cycle")
    private UserCycle userCycle;

    @ManyToOne
    @JoinColumn(name = "id_student_cycle")
    private StudentCycle studentCycle;
}
