package pe.com.edu.prisma.imports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.common.BaseEntity;
import pe.com.edu.prisma.exams.Exam;

/**
 * One uploaded spreadsheet waiting for a person (REQ-IMPORTS-01): nothing in
 * it becomes real data until the batch is confirmed, all or nothing. A
 * students batch may carry the stage its students will be enrolled in; an
 * exam results batch, its exam and that exam's stage.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "import_batch")
public class ImportBatch extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ImportBatchType type;

    // students batches: where to enroll on confirm; null = only register
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", updatable = false)
    private Stage stage;

    // exam results batches: the exam the results are for
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", updatable = false)
    private Exam exam;

    // exam results batches: rows of the file that were not students
    @Column(name = "skipped_rows", nullable = false, updatable = false)
    private int skippedRows;

    @Column(name = "file_name", nullable = false, updatable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportBatchStatus status = ImportBatchStatus.PENDING_REVIEW;

    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private Long uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @OneToMany(mappedBy = "batch")
    @OrderBy("rowNumber")
    private List<ImportRow> rows = new ArrayList<>();
}
