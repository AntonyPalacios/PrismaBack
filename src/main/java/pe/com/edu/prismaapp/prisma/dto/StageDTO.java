package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.com.edu.prismaapp.prisma.entities.Stage;

import java.util.Date;

@Data
@NoArgsConstructor
public class StageDTO {

    private Long id;
    private String name;
    @JsonFormat(pattern="dd/MM/yyyy")
    private Date startDate;
    @JsonFormat(pattern="dd/MM/yyyy")
    private Date endDate;
    private Long idCycle;
    @JsonProperty("isCurrent")
    private boolean isCurrent;

    public StageDTO(Stage stage) {
        this.id = stage.getId();
        this.name = stage.getName();
        this.startDate = stage.getStartDate();
        this.endDate = stage.getEndDate();
        this.idCycle = stage.getCycle().getId();
        this.isCurrent = stage.isCurrent();
    }
}
