package pe.com.edu.prisma.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;

/** Email is not editable: it is the identity Google logs in with. */
public record UpdateUserRequest(

        @NotBlank(message = "Ingresa los nombres del usuario.")
        @Size(max = 150, message = "Los nombres son demasiado largos.")
        @Sensitive(LogMask.NAME)
        String firstName,

        @NotBlank(message = "Ingresa el apellido paterno del usuario.")
        @Size(max = 100, message = "El apellido paterno es demasiado largo.")
        @Sensitive(LogMask.NAME)
        String lastNamePat,

        @Size(max = 100, message = "El apellido materno es demasiado largo.")
        @Sensitive(LogMask.NAME)
        String lastNameMat,

        @NotEmpty(message = "Selecciona al menos un rol.")
        Set<@Pattern(regexp = "ADMIN|TUTOR|COORDINADOR", message = "Uno de los roles no es válido.") String> roles
) {
}
