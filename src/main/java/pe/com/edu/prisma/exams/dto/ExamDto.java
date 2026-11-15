package pe.com.edu.prisma.exams.dto;

import java.time.LocalDate;

/** An exam of a stage; {@code hasResults} = its type can no longer change and it can't be deleted. */
public record ExamDto(
        Long id,
        Long stageId,
        String name,
        LocalDate examDate,
        Long examTypeId,
        String examTypeName,
        boolean hasResults
) {
}
