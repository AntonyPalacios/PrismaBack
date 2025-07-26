package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "TUSER")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long id;

    private String name;
//    private String lastname;
    private String email;
    private boolean active;
    private String picture; // URL de la foto de perfil de Google
    private String provider; // "google"
    private String providerId; // ID único de Google de Google

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "TROLEUSER",
            joinColumns = @JoinColumn(name = "id_user"),
            inverseJoinColumns = @JoinColumn(name = "id_role"))
    private List<Role> roles;

}
