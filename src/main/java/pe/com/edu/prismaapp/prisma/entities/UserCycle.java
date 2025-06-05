package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "TUSERCYCLE")
public class UserCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user_cycle")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_user")
    private User user;

    @ManyToOne
    @JoinColumn(name = "id_cycle")
    private Cycle cycle;

    @ManyToOne
    @JoinColumn(name = "id_role")
    private Role role;
}
