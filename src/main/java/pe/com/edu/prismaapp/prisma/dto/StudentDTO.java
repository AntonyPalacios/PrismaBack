package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private Long id;

    @NotBlank
    @Size(min = 2, max = 50)
    private String name;

    @Email
    @NotBlank
    private String email;

    @Size(max = 9)
    private String phone;

    @NotBlank
    @Size(max = 8)
    private String dni;
    private Long tutorId;
    private Long areaId;
    private Long stageId;
    @JsonProperty("isActive")
    private boolean isActive;
}
