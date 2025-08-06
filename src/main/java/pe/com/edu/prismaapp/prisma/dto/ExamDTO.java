package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExamDTO {
    private Long id;

    @NotBlank
    @Size(min = 3, max = 15)
    private String name;
    private String cycle;
    private String stage;
    private Long stageId;
    @JsonFormat(pattern="dd/MM/yyyy")
    private Date date;

}
