package pe.com.edu.prisma.imports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import pe.com.edu.prisma.common.BaseEntity;
import pe.com.edu.prisma.students.Student;

/**
 * One spreadsheet row of a batch. The parsed row and the suggested
 * candidates are kept as JSON (the service reads and writes them): they are
 * a staging area, not data the rest of the system queries.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "import_row")
public class ImportRow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false, updatable = false)
    private ImportBatch batch;

    // the row's number in the spreadsheet, as the user sees it
    @Column(name = "row_number", nullable = false, updatable = false)
    private int rowNumber;

    // JSON of the parsed row (StudentRowPayload / ResultRowPayload); a results
    // row also keeps there what the review decided (enroll, new student's names)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String rawPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 20)
    private ImportRowStatus status = ImportRowStatus.UNRESOLVED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_student_id")
    private Student matchedStudent;

    // JSON array of the ids of students that may be the same person
    @Column(name = "candidate_matches", columnDefinition = "jsonb", updatable = false)
    @ColumnTransformer(write = "?::jsonb")
    private String candidateMatches;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
}
