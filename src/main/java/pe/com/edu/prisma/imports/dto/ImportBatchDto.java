package pe.com.edu.prisma.imports.dto;

import java.time.OffsetDateTime;
import java.util.List;
import pe.com.edu.prisma.imports.ImportBatchStatus;
import pe.com.edu.prisma.imports.ImportBatchType;

/**
 * A batch with its counts and, for the review, its rows ({@code rows} is
 * empty in listings). {@code stageLabel} = "Ciclo · Etapa" where the
 * students will be enrolled, or null when they are only registered.
 */
public record ImportBatchDto(
        Long id,
        ImportBatchType type,
        ImportBatchStatus status,
        String fileName,
        Long stageId,
        String stageLabel,
        OffsetDateTime uploadedAt,
        int totalRows,
        int newRows,
        int matchedRows,
        int unresolvedRows,
        int ignoredRows,
        List<ImportRowDto> rows
) {
}
