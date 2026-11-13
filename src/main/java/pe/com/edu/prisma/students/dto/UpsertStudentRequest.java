package pe.com.edu.prisma.students.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Locale;
import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;

/**
 * Register or edit a student. Every field is personal data of a minor, so
 * every one is marked {@link Sensitive}: the logs only ever show it masked.
 */
public record UpsertStudentRequest(

        @Sensitive(LogMask.NAME)
        @NotBlank(message = "Ingresa los nombres del alumno.")
        @Size(max = 150, message = "Los nombres son demasiado largos.")
        String firstName,

        @Sensitive(LogMask.NAME)
        @NotBlank(message = "Ingresa el apellido paterno del alumno.")
        @Size(max = 100, message = "El apellido paterno es demasiado largo.")
        String lastNamePat,

        @Sensitive(LogMask.NAME)
        @Size(max = 100, message = "El apellido materno es demasiado largo.")
        String lastNameMat,

        // Peruvian DNI (8 digits) or a foreigner's card: 8 to 12 letters or digits
        @Sensitive(LogMask.DNI)
        @Pattern(regexp = "[A-Z0-9]{8,12}", message = "El DNI debe tener entre 8 y 12 letras o números.")
        String dni,

        @Sensitive(LogMask.EMAIL)
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 255, message = "El correo es demasiado largo.")
        String email,

        @Sensitive(LogMask.HIDDEN)
        @Size(max = 30, message = "El teléfono es demasiado largo.")
        String phone,

        @Sensitive(LogMask.HIDDEN)
        @PastOrPresent(message = "La fecha de nacimiento no puede ser futura.")
        LocalDate birthDate
) {

    // Optional text arrives as "" from a form: store it as "nothing" (null).
    // The DNI is compared as text, so it is kept trimmed and in upper case.
    public UpsertStudentRequest {
        lastNameMat = blankToNull(lastNameMat);
        dni = blankToNull(dni);
        dni = dni == null ? null : dni.toUpperCase(Locale.ROOT);
        email = blankToNull(email);
        phone = blankToNull(phone);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
