package pe.com.edu.prisma.security;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import pe.com.edu.prisma.users.User;

/**
 * Bridges the Google {@code OAuth2User} (raw attributes from Google) with
 * our own {@code User} row, so the login success handler has the app_user
 * id it needs to mint a JWT without re-querying by email.
 */
public class PrismaOAuth2User implements OAuth2User {

    private final OAuth2User delegate;
    private final User user;

    public PrismaOAuth2User(OAuth2User delegate, User user) {
        this.delegate = delegate;
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return new CustomUserDetails(user).getAuthorities();
    }

    @Override
    public String getName() {
        return user.getEmail();
    }
}
