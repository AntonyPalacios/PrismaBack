package pe.com.edu.prisma.imports.dto;

import java.time.OffsetDateTime;
import java.util.List;
import pe.com.edu.prisma.imports.ImportBatchStatus;
import pe.com.edu.prisma.imports.ImportBatchType;

/**
 * A batch with its counts and, for the review, its rows ({@code rows} is
 * empty in listings). {@code stageLabel} = "Ciclo · Etapa" where the
 * students will be enrolled (null when a students batch only registers).
 * Exam results batches carry their exam ({@code examLabel} = "Simulacro ·
 * Tipo") and {@code skippedRows}, the rows of the file that were not
 * students; students batches have them null and 0.
 */
public record ImportBatchDto(
        Long id,
        ImportBatchType type,
        ImportBatchStatus status,
        String fileName,
        Long stageId,
        String stageLabel,
        Long examId,
        String examLabel,
        OffsetDateTime uploadedAt,
        int totalRows,
        int newRows,
        int matchedRows,
        int unresolvedRows,
        int ignoredRows,
        int skippedRows,
        List<ImportRowDto> rows
) {
}
