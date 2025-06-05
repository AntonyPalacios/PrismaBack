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
}
