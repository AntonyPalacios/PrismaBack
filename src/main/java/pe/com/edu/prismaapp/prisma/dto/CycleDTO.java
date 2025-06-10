package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.com.edu.prismaapp.prisma.entities.Cycle;

import java.util.Date;

@Data
@NoArgsConstructor
public class CycleDTO {

    private Long id;
    private String name;
    @JsonFormat(pattern="dd/MM/yyyy")
    private Date startDate;
    @JsonFormat(pattern="dd/MM/yyyy")
    private Date endDate;

    public CycleDTO(Cycle cycle) {
        this.id = cycle.getId();
        this.name = cycle.getName();
        this.startDate = cycle.getStartDate();
        this.endDate = cycle.getEndDate();
    }

}
