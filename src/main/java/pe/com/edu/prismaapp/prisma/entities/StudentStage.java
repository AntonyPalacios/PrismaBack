package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;

@Entity
@Table(
        name = "TSTUDENTSTAGE",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"id_student", "id_stage"})
        }
)
public class StudentStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn(name = "id_student_stage")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_student")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "id_stage")
    private Stage stage;

    private boolean active;
}
