package pe.com.edu.prisma.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.edu.prisma.common.FullAuditEntity;

/**
 * Registration is admin-controlled, not self-service: a Google account
 * only becomes a session if its email already matches a row here (see
 * the security module's GoogleLoginService) — same policy the old
 * system had, just enforced in one place instead of implied.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_user")
public class User extends FullAuditEntity {

    /**
     * Project agreement (immutable): this account is always an active ADMIN.
     * Seeded by V3; no flow may deactivate it or take that role away.
     */
    public static final String PERMANENT_ADMIN_EMAIL = "apalaciosc0109@gmail.com";

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name_pat", nullable = false)
    private String lastNamePat;

    @Column(name = "last_name_mat")
    private String lastNameMat;

    @Column(name = "google_sub")
    private String googleSub;

    @Column(name = "photo_url", length = 1024)
    private String photoUrl;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    /** Both surnames, e.g. "Palacios Coronado" — used for "Apellidos, Nombres" displays. */
    public String getLastNames() {
        return lastNameMat == null || lastNameMat.isBlank() ? lastNamePat : lastNamePat + " " + lastNameMat;
    }

    public boolean isPermanentAdmin() {
        return PERMANENT_ADMIN_EMAIL.equalsIgnoreCase(email);
    }

    public String getFullName() {
        return lastNameMat == null || lastNameMat.isBlank()
                ? firstName + " " + lastNamePat
                : firstName + " " + lastNamePat + " " + lastNameMat;
    }
}
