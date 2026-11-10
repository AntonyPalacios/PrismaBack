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

/**
 * What a student applies for (Ciencias, Letras, Arquitectura, Biomédica,
 * Derecho, Artes). Several careers can sit the same exam type (Biomédica
 * takes the Ciencias exam). Changing a career's exam type never rewrites
 * history: results stay pinned to exam.exam_type_id.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "career")
public class Career extends BaseEntity {

    // unique ignoring case: enforced by ux_career_name_lower (V5) and checked in the service
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String abbreviation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_type_id", nullable = false)
    private ExamType examType;

    @Column(nullable = false)
    private boolean active = true;
}
