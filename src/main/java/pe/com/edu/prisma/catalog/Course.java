package pe.com.edu.prisma.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.edu.prisma.common.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "course")
public class Course extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String abbreviation;

    @Column(nullable = false)
    private boolean active = true;
}
