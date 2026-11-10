package pe.com.edu.prisma.catalog;

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
import pe.com.edu.prisma.common.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam_type")
public class ExamType extends BaseEntity {

    // unique ignoring case: enforced by ux_exam_type_name_lower (V4) and checked in the service
    @Column(nullable = false, length = 50)
    private String name; // Ciencias, Letras, Arquitectura

    @Column(nullable = false)
    private boolean active = true;

    // which courses this career track actually evaluates (RF-03.02)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "exam_type_course",
            joinColumns = @JoinColumn(name = "exam_type_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"))
    private Set<Course> courses = new HashSet<>();
}
