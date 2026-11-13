package pe.com.edu.prisma.tutoring.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTutorRequest(
        @NotNull(message = "Elige el tutor.")
        Long tutorUserId
) {
}
