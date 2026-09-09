package pe.com.edu.prisma.security;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.UserRepository;

/**
 * Registration is admin-controlled (see {@link User}): logging in with
 * Google only succeeds if that email is already a row in app_user and
 * is active. Anyone else authenticates fine with Google but never gets
 * a Prisma session — same policy the original system had.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("user_not_registered"),
                        "No hay una cuenta de Prisma activa para " + email));

        return new PrismaOAuth2User(oAuth2User, user);
    }
}
