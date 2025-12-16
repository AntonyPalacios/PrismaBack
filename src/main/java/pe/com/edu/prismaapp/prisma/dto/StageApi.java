package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.Cycle;
import pe.com.edu.prismaapp.prisma.entities.Stage;

import java.util.Date;

public interface StageApi {

    record Create(@NotBlank @Size(min = 4, max = 15) String name,
                  @JsonFormat(pattern="dd/MM/yyyy") Date startDate,
                  @JsonFormat(pattern="dd/MM/yyyy") Date endDate,
                  @NotNull Long idCycle){}

    record Update(@NotBlank Long id,
                  @NotBlank @Size(min = 4, max = 15) String name,
                  @JsonFormat(pattern="dd/MM/yyyy") Date startDate,
                  @JsonFormat(pattern="dd/MM/yyyy") Date endDate,
                  @NotNull Long idCycle){}

    record Response(
          @NotBlank Long id,
          @NotBlank @Size(min = 4, max = 15) String name,
          @JsonFormat(pattern="dd/MM/yyyy") Date startDate,
          @JsonFormat(pattern="dd/MM/yyyy") Date endDate,
          @NotNull Long idCycle,
          @JsonProperty("isCurrent") boolean isCurrent){

        public static StageApi.Response from(Stage stage) {
            return new StageApi.Response(
                    stage.getId(),
                    stage.getName(),
                    stage.getStartDate(),
                    stage.getEndDate(),
                    stage.getCycle().getId(),
                    stage.isCurrent()
            );
        }
    }
}
