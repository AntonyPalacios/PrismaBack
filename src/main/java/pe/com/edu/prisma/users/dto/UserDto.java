package pe.com.edu.prisma.users.dto;

import java.util.List;

/**
 * {@code permanentAdmin} lets the UI hide "Desactivar" and lock the ADMIN
 * role for the permanent admin account (the backend enforces it anyway).
 */
public record UserDto(
        Long id,
        String email,
        String firstName,
        String lastNamePat,
        String lastNameMat,
        List<String> roles,
        boolean active,
        String photoUrl,
        boolean permanentAdmin
) {
}
