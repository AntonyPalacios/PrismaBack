package pe.com.edu.prisma.exams.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * A mock exam with its exam types (catalog order). {@code hasResults} =
 * some of its exams has results, so the mock exam can't be deleted.
 */
public record MockExamDto(
        Long id,
        Long stageId,
        String name,
        LocalDate examDate,
        boolean hasResults,
        List<ExamDto> exams
) {
}
