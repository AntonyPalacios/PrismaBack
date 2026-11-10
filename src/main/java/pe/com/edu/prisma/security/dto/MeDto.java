package pe.com.edu.prisma.security.dto;

import java.util.List;

/**
 * Who is logged in, as the frontend sees it. {@code lastName} holds both
 * surnames ("Palacios Coronado") so the UI can show "Apellidos, Nombres".
 */
public record MeDto(Long id, String email, String firstName, String lastName, List<String> roles,
        String photoUrl) {
}
