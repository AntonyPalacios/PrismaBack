package pe.com.edu.prisma.exams;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.edu.prisma.catalog.Area;
import pe.com.edu.prisma.common.BaseEntity;

/** The score of one area (Lectura, Matemática) in a result, as the academy's file brings it (V13). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam_area_result")
public class ExamAreaResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_result_id", nullable = false, updatable = false)
    private ExamResult examResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false, updatable = false)
    private Area area;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;
}
