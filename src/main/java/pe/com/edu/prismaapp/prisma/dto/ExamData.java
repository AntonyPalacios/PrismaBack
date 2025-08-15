package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ExamData {
    private Long id;
    private String name;
    @JsonFormat(pattern="dd/MM/yyyy")
    private Date date;
}
