package pe.com.edu.prisma.students.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;

/** A guardian's contact data: personal data, every field {@link Sensitive}. */
public record UpsertParentRequest(

        @Sensitive(LogMask.NAME)
        @NotBlank(message = "Ingresa el nombre completo del apoderado.")
        @Size(max = 200, message = "El nombre es demasiado largo.")
        String fullName,

        // 7 to 15 digits, maybe with a leading "+" and single spaces between them
        @Sensitive(LogMask.HIDDEN)
        @NotBlank(message = "Ingresa el celular del apoderado.")
        @Pattern(regexp = "\\+?[0-9]( ?[0-9]){6,14}", message = "El celular debe tener entre 7 y 15 dígitos.")
        String phone,

        @Sensitive(LogMask.EMAIL)
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 255, message = "El correo es demasiado largo.")
        String email
) {

    // trimmed, runs of spaces collapsed; an empty email is "nothing"
    public UpsertParentRequest {
        fullName = fullName == null ? null : fullName.trim().replaceAll("\\s+", " ");
        phone = phone == null ? null : phone.trim().replaceAll("\\s+", " ");
        email = email == null || email.isBlank() ? null : email.trim();
    }
}
