package pe.com.edu.prisma.goals.dto;

import java.time.LocalDate;

/** A mock exam of the active cycle that goals can aim at. */
public record GoalMockExamDto(Long id, String name, LocalDate examDate, String stageName) {
}
