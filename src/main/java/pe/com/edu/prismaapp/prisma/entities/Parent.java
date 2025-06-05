package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "TPARENT")
public class Parent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parent")
    private Long id;

    private String name;
    private String relationship;



}
