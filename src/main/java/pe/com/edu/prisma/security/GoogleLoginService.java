package pe.com.edu.prisma.security;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.UserRepository;

/**
 * The business half of a Google login, kept apart from Spring's OIDC
 * plumbing so it can be tested without talking to Google.
 *
 * Registration is admin-managed: a Google account only gets a Prisma
 * session if its (verified) email already belongs to an active app_user.
 * Unknown and inactive users get the same error code on purpose, so the
 * login page never reveals whether an email exists in the system.
 */
@Service
public class GoogleLoginService {

    /** OAuth2 error code the failure handler forwards to the frontend. */
    public static final String NOT_REGISTERED = "not_registered";

    private final UserRepository userRepository;

    public GoogleLoginService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User registerLogin(String email, boolean emailVerified, String googleSub, String photoUrl) {
        if (email == null || !emailVerified) {
            throw notRegistered();
        }
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .orElseThrow(GoogleLoginService::notRegistered);

        // Refreshed on every login so the avatar follows the Google account.
        user.setPhotoUrl(photoUrl);
        if (googleSub != null) {
            user.setGoogleSub(googleSub);
        }
        return user;
    }

    private static OAuth2AuthenticationException notRegistered() {
        return new OAuth2AuthenticationException(
                new OAuth2Error(NOT_REGISTERED), "No hay una cuenta de Prisma activa para este correo");
    }
}
