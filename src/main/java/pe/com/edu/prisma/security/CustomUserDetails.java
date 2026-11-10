package pe.com.edu.prisma.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.com.edu.prisma.users.User;

/**
 * Wraps our {@code User} entity so Spring Security can work with it,
 * while also exposing {@code getId()} for {@link AuthenticatedUser} —
 * that's the one method {@code common}'s auditing bean actually needs.
 */
public class CustomUserDetails implements UserDetails, AuthenticatedUser {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Long getId() {
        return user.getId();
    }

    public String getFullName() {
        return user.getFullName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .toList();
    }

    @Override
    public String getPassword() {
        return null; // login is Google OAuth2 only — there is no local password
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

    public static List<String> roleNames(User user) {
        return user.getRoles().stream().map(pe.com.edu.prisma.users.Role::getName).toList();
    }
}
