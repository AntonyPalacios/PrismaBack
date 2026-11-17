package pe.com.edu.prisma.goals.dto;

import java.time.LocalDate;

/**
 * One of the student's goals in the active cycle, with how it went:
 * {@code attended} and {@code actual} are null while the exam has no result
 * for them, {@code met} is null then and when they did not attend.
 */
public record StudentGoalDto(
        Long id,
        Long mockExamId,
        String mockExamName,
        LocalDate examDate,
        String examTypeName,
        Long areaId,
        String areaName,
        int targetCorrect,
        Integer actual,
        Boolean attended,
        Boolean met
) {
}
