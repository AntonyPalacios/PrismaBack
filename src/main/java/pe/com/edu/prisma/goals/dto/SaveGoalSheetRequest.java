package pe.com.edu.prisma.goals.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** A group's goals for one mock exam; a null {@code targetCorrect} removes that goal. Only ids and counts. */
public record SaveGoalSheetRequest(
        @NotNull(message = "Elige el simulacro.")
        Long mockExamId,

        @NotNull(message = "Faltan las metas.")
        List<@Valid @NotNull Entry> goals
) {

    public record Entry(
            @NotNull(message = "Indica la matrícula del alumno.")
            Long enrollmentId,

            @NotNull(message = "Indica el área de la meta.")
            Long areaId,

            @Min(value = 0, message = "La meta no puede ser negativa.")
            @Max(value = 999, message = "La meta es demasiado grande.")
            Integer targetCorrect
    ) {
    }
}
