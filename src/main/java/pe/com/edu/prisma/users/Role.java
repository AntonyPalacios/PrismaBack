package pe.com.edu.prisma.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.edu.prisma.common.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "role")
public class Role extends BaseEntity {

    public static final String ADMIN = "ADMIN";
    public static final String TUTOR = "TUTOR";
    public static final String COORDINADOR = "COORDINADOR";

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
