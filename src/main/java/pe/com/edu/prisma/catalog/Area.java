package pe.com.edu.prisma.catalog;

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
@Table(name = "area")
public class Area extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String abbreviation;

    @Column(nullable = false)
    private boolean active = true;
}
