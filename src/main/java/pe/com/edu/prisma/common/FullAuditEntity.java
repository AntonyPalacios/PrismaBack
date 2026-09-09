package pe.com.edu.prisma.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * For mutable entities (student, cycle, stage, exam, exam_result,
 * student_stage) — answers "who touched this last" (RF-01.04). This is
 * the ceiling of the audit strategy we chose: last-writer tracking, not
 * a full change-event log.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class FullAuditEntity extends CreatedAuditEntity {

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;
}
