package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.Cycle;

import java.util.Date;

public interface CycleApi {
    record Create(@NotBlank @Size(min = 4, max = 15) String name,
                  @JsonFormat(pattern = "dd/MM/yyyy") Date startDate,
                  @JsonFormat(pattern = "dd/MM/yyyy") Date endDate){}

    record Update(@NotBlank Long id,
                  @NotBlank @Size(min = 4, max = 15) String name,
                  @JsonFormat(pattern = "dd/MM/yyyy") Date startDate,
                  @JsonFormat(pattern = "dd/MM/yyyy") Date endDate){}

    record Response( @NotBlank Long id,
                     @NotBlank @Size(min = 4, max = 15) String name,
                     @JsonFormat(pattern = "dd/MM/yyyy") Date startDate,
                     @JsonFormat(pattern = "dd/MM/yyyy") Date endDate,
                     @JsonProperty("isCurrent") boolean isCurrent){

        public static Response from(Cycle cycle) {
            return new Response(
                    cycle.getId(),
                    cycle.getName(),
                    cycle.getStartDate(),
                    cycle.getEndDate(),
                    cycle.isCurrent()
            );
        }
    }
}
