package pe.com.edu.prismaapp.prisma.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.User;

public interface UserApi {

    @Schema(name = "CreateUserRequest", description = "Datos requeridos para crear un usuario")
    record Create(
            @NotBlank
            @Size(min = 2, max = 50)
            @Schema(description = "Nombre del usuario", example = "Silvia Valle")
            String name,

            @Email
            @NotBlank
            @Schema(description = "Correo del usuario con el que ingresará al sistema (Google)",
                    example = "silvia.valle@prisma.edu.pe")
            String email,

            @JsonProperty("isAdmin")
            @Schema(description = "Flag que indica si es administrador", example = "false")
            boolean isAdmin,

            @JsonProperty("isActive")
            @Schema(description = "Flag que indica si esta activo", example = "true")
            boolean isActive,

            @JsonProperty("isTutor")
            @Schema(description = "Flag que indica si es tutor", example = "true")
            boolean isTutor
    ) {
    }

    @Schema(name = "UpdateUserRequest", description = "Datos requeridos para actualizar un usuario")
    record Update(
            @NotNull
            @Schema(description = "Id del usuario", example = "5")
            Long id,

            @NotBlank
            @Size(min = 2, max = 50)
            @Schema(description = "Nombre del usuario", example = "Silvia Valle")
            String name,

            @Email
            @NotBlank
            @Schema(description = "Correo del usuario con el que ingresará al sistema (Google)",
                    example = "silvia.valle@prisma.edu.pe")
            String email,

            @JsonProperty("isAdmin")
            @Schema(description = "Flag que indica si es administrador", example = "false")
            boolean isAdmin,

            @JsonProperty("isActive")
            @Schema(description = "Flag que indica si esta activo", example = "true")
            boolean isActive,

            @JsonProperty("isTutor")
            @Schema(description = "Flag que indica si es tutor", example = "true")
            boolean isTutor
    ) {
    }

    @Schema(name = "CurrentUserResponse", description = "Respuesta con los detalles del usuario actual")
    record CurrentUser(
            @NotNull
            @Schema(description = "Id del usuario", example = "5")
            Long id,

            @NotBlank
            @Size(min = 2, max = 50)
            @Schema(description = "Nombre del usuario", example = "Silvia Valle")
            String name,

            @Email
            @NotBlank
            @Schema(description = "Correo del usuario con el que ingresará al sistema (Google)",
                    example = "silvia.valle@prisma.edu.pe")
            String email,

            @JsonProperty("isActive")
            @Schema(description = "Flag que indica si esta activo", example = "true")
            boolean isActive,

            @Schema(description = "Url de su foto de perfil de google")
            String picture
    ) {
    }

    @Schema(name = "UserResponse", description = "Respuesta con los detalles del usuario ")
    record Response(
            @NotNull
            @Schema(description = "Id del usuario", example = "5")
            Long id,

            @NotBlank
            @Size(min = 2, max = 50)
            @Schema(description = "Nombre del usuario", example = "Silvia Valle")
            String name,

            @Email
            @NotBlank
            @Schema(description = "Correo del usuario con el que ingresará al sistema (Google)",
                    example = "silvia.valle@prisma.edu.pe")
            String email,

            @JsonProperty("isAdmin")
            @Schema(description = "Flag que indica si es administrador", example = "false")
            boolean isAdmin,

            @JsonProperty("isActive")
            @Schema(description = "Flag que indica si esta activo", example = "true")
            boolean isActive,

            @JsonProperty("isTutor")
            @Schema(description = "Flag que indica si es tutor", example = "true")
            boolean isTutor
    ) {
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
