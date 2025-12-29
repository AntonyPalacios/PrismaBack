package pe.com.edu.prismaapp.prisma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.Goal;

public interface GoalApi {

    @Schema(name = "CreateGoalRequest", description = "Datos requeridos para crear una meta")
    record Create(
            @NotNull
            @Schema(description = "Id del examen", example = "3")
            Long examId,

            @NotNull
            @Schema(description = "Id del alumno", example = "25")
            Long studentId,

            @NotNull
            @Size(max = 1)
            @Schema(description = "Porcentaje de mejora", example = "0.15")
            double scoreGoal
    ) {
    }

    @Schema(name = "UpdateGoalRequest", description = "Datos requeridos para actualizar una meta")
    record Update(
            @NotNull
            @Schema(description = "Id de la meta", example = "3")
            Long id,

            @NotNull
            @Size(max = 1)
            @Schema(description = "Porcentaje de mejora", example = "0.15")
            double scoreGoal
    ) {
    }

    @Schema(name = "GoalResponse", description = "Respuesta con detalles de la meta")
    record Response(
            @NotNull
            @Schema(description = "Id de la meta", example = "3")
            Long id,

            @NotNull
            @Schema(description = "Id del examen", example = "3")
            Long examId,

            @NotNull
            @Schema(description = "Id del alumno", example = "25")
            Long studentId,

            @NotNull
            @Size(max = 1)
            @Schema(description = "Porcentaje de mejora", example = "0.15")
            double scoreGoal
    ) {

        public static GoalApi.Response from(Goal goal) {
            return new GoalApi.Response(
                    goal.getId(),
                    goal.getExam().getId(),
                    goal.getStudent().getId(),
                    goal.getScoreGoal()
            );
        }
    }
}
