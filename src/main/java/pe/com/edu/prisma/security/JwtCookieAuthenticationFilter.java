package pe.com.edu.prisma.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.com.edu.prisma.common.logging.TraceIds;
import pe.com.edu.prisma.config.AppProperties;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.UserRepository;

/**
 * Reads the JWT from an httpOnly cookie (never an Authorization header,
 * never a URL query param — both were flagged in the old system's
 * security audit) and, if valid, loads the user fresh from the database
 * so a deactivated account or a role change takes effect on the very
 * next request rather than only after the token expires.
 */
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final String cookieName;

    public JwtCookieAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
            AppProperties appProperties) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.cookieName = appProperties.jwt().cookieName();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, jakarta.servlet.ServletException {

        readCookie(request)
                .flatMap(jwtService::parseUserId)
                .flatMap(userRepository::findById)
                .filter(User::isActive)
                .ifPresent(user -> {
                    CustomUserDetails principal = new CustomUserDetails(user);
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    // printed on every log line of this request (the id, never the email)
                    MDC.put(TraceIds.USER_ID, String.valueOf(user.getId()));
                });

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIds.USER_ID); // the thread goes back to the pool
        }
    }

    private Optional<String> readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
