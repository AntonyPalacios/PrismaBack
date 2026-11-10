package pe.com.edu.prisma.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.edu.prisma.security.dto.MeDto;
import pe.com.edu.prisma.users.User;

/**
 * Session endpoints for the SPA. The JWT lives in an httpOnly cookie the
 * frontend can't read, so {@code /me} is how it learns who is logged in.
 * Logout ({@code POST /api/v1/auth/logout}) is handled by Spring
 * Security's logout filter — see SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @GetMapping("/me")
    public MeDto me(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = principal.getUser();
        return new MeDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastNames(),
                CustomUserDetails.roleNames(user).stream().sorted().toList(),
                user.getPhotoUrl());
    }
}
