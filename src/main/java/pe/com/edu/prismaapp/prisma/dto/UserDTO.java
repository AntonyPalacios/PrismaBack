package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String name;
//    private String lastname;
    private String email;
//    private String password;
    @JsonProperty("isAdmin")
    private boolean isAdmin;
    @JsonProperty("isActive")
    private boolean isActive;
    @JsonProperty("isTutor")
    private boolean isTutor;
}
