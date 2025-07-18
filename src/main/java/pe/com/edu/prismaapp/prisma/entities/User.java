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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "TROLEUSER",
            joinColumns = @JoinColumn(name = "id_role"),
            inverseJoinColumns = @JoinColumn(name = "id_user"))
    private Set<Role> roles;

    public User(String email, String name, String picture, String provider, String providerId) {
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.provider = provider;
        this.providerId = providerId;
        this.active = true; // Por defecto activo para nuevos usuarios OAuth
        this.roles = new HashSet<>(); // Asegura que se inicialice
    }
}
