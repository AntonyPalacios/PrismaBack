package pe.com.edu.prismaapp.prisma.dto;

import jakarta.validation.constraints.NotNull;
import pe.com.edu.prismaapp.prisma.entities.Goal;

public interface GoalApi {
    record Create(@NotNull Long examId,
                  Long studentId,
                  double scoreGoal){}

    record Update(@NotNull Long id,
                  double scoreGoal){}

    record Response( @NotNull Long id,
                     @NotNull Long examId,
                     @NotNull Long studentId,
                     double scoreGoal){

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
