package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;

@Entity
@Table(
        name = "TSTUDENTCYCLE",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"id_student", "id_cycle"})
        }
)
public class StudentCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn(name = "id_student_cycle")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_student")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "id_cycle")
    private Cycle cycle;

    private boolean active;
}
