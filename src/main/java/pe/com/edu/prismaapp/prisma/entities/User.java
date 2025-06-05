package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "TUSER")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long id;

    private String firstname;
    private String lastname;
    private String email;
    private String password;

    @OneToMany(mappedBy = "tutor")
    private List<Student> students;
}
