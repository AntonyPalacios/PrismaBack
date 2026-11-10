package pe.com.edu.prisma.security;

import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import pe.com.edu.prisma.users.User;

/**
 * The Google identity plus our own app_user row, so the success handler
 * can mint the JWT for the right user id without querying again.
 * Authorities come from Prisma roles, never from Google.
 */
public class PrismaOidcUser extends DefaultOidcUser {

    private final User user;

    public PrismaOidcUser(OidcUser googleUser, User user) {
        super(new CustomUserDetails(user).getAuthorities(), googleUser.getIdToken(), googleUser.getUserInfo(),
                "email");
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
