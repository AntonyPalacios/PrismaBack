package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.Cycle;
import pe.com.edu.prismaapp.prisma.entities.User;

public interface UserApi {
    record Create (@NotBlank @Size(min = 2, max = 50) String name,
                   @Email @NotBlank String email,
                   @JsonProperty("isAdmin") boolean isAdmin,
                   @JsonProperty("isActive") boolean isActive,
                   @JsonProperty("isTutor") boolean isTutor){}

    record Update (@NotBlank Long id,
                   @NotBlank @Size(min = 2, max = 50) String name,
                   @Email @NotBlank String email,
                   @JsonProperty("isAdmin") boolean isAdmin,
                   @JsonProperty("isActive") boolean isActive,
                   @JsonProperty("isTutor") boolean isTutor){}

    record CurrentUser(@NotBlank Long id,
                       @NotBlank @Size(min = 2, max = 50) String name,
                       @Email @NotBlank String email,
                       @JsonProperty("isActive") boolean isActive,
                       String picture){}

    record Response (@NotBlank Long id,
                     @NotBlank @Size(min = 2, max = 50) String name,
                     @Email @NotBlank String email,
                     @JsonProperty("isAdmin") boolean isAdmin,
                     @JsonProperty("isActive") boolean isActive,
                     @JsonProperty("isTutor") boolean isTutor){
        public static UserApi.Response from(User user) {
            return new UserApi.Response(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN")),
                    user.isActive(),
                    user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_TUTOR"))
            );
        }
    }
}
