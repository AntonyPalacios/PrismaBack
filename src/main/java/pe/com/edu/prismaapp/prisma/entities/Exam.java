package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "TEXAM")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_exam")
    private Long id;

    private String name;
    private Date date;

    @ManyToOne
    @JoinColumn(name = "id_cycle")
    private Cycle cycle;
}
