package pe.com.edu.prisma.exams.dto;

/**
 * One exam type of a mock exam; {@code hasResults} = it can no longer be
 * removed; {@code pendingImportBatchId} = a results import of it waits in review.
 */
public record ExamDto(
        Long id,
        Long examTypeId,
        String examTypeName,
        boolean hasResults,
        Long pendingImportBatchId
) {
}
