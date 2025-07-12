package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String dni;
    private Long tutorId;
    private Long areaId;
    @JsonProperty("isActive")
    private boolean isActive;
}
