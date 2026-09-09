package pe.com.edu.prisma.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.com.edu.prisma.security.AuthenticatedUser;

@Configuration
public class AuditorAwareConfig {

    /**
     * Feeds {@code @CreatedBy}/{@code @LastModifiedBy}. Returns empty for
     * anonymous/system contexts (Flyway seed data, scheduled jobs, tests
     * with no logged-in user) — those columns simply stay NULL, which is
     * allowed on every audited table except the FK constraint target.
     */
    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            if (authentication.getPrincipal() instanceof AuthenticatedUser user) {
                return Optional.ofNullable(user.getId());
            }
            return Optional.empty();
        };
    }
}
