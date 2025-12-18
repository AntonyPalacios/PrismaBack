package pe.com.edu.prismaapp.prisma.dto.exam;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.Exam;

import java.util.Date;

public interface ExamApi {
    record Create(@NotBlank @Size(min = 3, max = 15) String name,
                  @NotBlank Long stageId,
                  @JsonFormat(pattern = "dd/MM/yyyy") Date date
    ) {
    }

    record Update(@NotBlank Long id,
                  @NotBlank @Size(min = 3, max = 15) String name,
                  @NotBlank Long stageId,
                  @JsonFormat(pattern = "dd/MM/yyyy") Date date) {
    }

    record ExamList(@NotBlank Long id,
                    @NotBlank @Size(min = 3, max = 15) String name,
                    @NotBlank Long stageId,
                    @JsonFormat(pattern = "dd/MM/yyyy") Date date,
                    String cycle,
                    String stage
    ) {
    }

    record ExamScore(String name,
                     double score,
                     double min,
                     double max,
                     double avg,
                     int merit
    ) {
    }

    record Response(@NotBlank Long id,
                    @NotBlank @Size(min = 3, max = 15) String name,
                    @NotBlank Long stageId,
                    @JsonFormat(pattern = "dd/MM/yyyy") Date date) {
        public static Response from(Exam exam) {
            return new ExamApi.Response(
                    exam.getId(),
                    exam.getName(),
                    exam.getStage().getId(),
                    exam.getDate()
            );
        }
    }
}
