package pe.com.edu.prisma.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import pe.com.edu.prisma.users.User;

/**
 * Google is an OpenID Connect provider and we request the {@code openid}
 * scope, so Spring routes the login through the OIDC user service — not
 * the plain OAuth2 one. (v2 originally hooked a plain OAuth2 service,
 * which Spring never called for Google, so the "registered users only"
 * check silently didn't run.)
 */
@Service
public class PrismaOidcUserService extends OidcUserService {

    private final GoogleLoginService googleLoginService;

    public PrismaOidcUserService(GoogleLoginService googleLoginService) {
        this.googleLoginService = googleLoginService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        User user = googleLoginService.registerLogin(
                oidcUser.getEmail(),
                Boolean.TRUE.equals(oidcUser.getEmailVerified()),
                oidcUser.getSubject(),
                oidcUser.getPicture());
        return new PrismaOidcUser(oidcUser, user);
    }
}
