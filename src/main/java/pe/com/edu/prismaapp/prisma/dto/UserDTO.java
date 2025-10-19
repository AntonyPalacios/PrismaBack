package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDTO {
    private Long id;

    @NotBlank
    @Size(min = 2, max = 50)
    private String name;
//    private String lastname;

    @Email
    @NotBlank
    private String email;
//    private String password;
    @JsonProperty("isAdmin")
    private boolean isAdmin;
    @JsonProperty("isActive")
    private boolean isActive;
    @JsonProperty("isTutor")
    private boolean isTutor;
    private String picture;
}
