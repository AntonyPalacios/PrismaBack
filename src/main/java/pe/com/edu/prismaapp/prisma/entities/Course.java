package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "TCOURSE")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_course")
    private Long id;

    private String name;
    private String abbreviation;
    private boolean main;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_parent_course") // Nuevo campo para cursos agrupados
    private Course parentCourse;
}
