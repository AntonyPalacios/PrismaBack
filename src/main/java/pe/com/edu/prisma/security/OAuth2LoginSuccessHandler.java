package pe.com.edu.prisma.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import pe.com.edu.prisma.config.AppProperties;

/**
 * Replaces the old flow of appending {@code ?token=...} to the redirect
 * URL (readable in browser history/referrer headers/proxy logs) with an
 * httpOnly, Secure cookie — JavaScript on the frontend never sees the
 * token, it just relies on the cookie being sent automatically.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final AppProperties appProperties;

    public OAuth2LoginSuccessHandler(JwtService jwtService, AppProperties appProperties) {
        this.jwtService = jwtService;
        this.appProperties = appProperties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        var prismaUser = ((PrismaOidcUser) authentication.getPrincipal()).getUser();

        String token = jwtService.issueToken(prismaUser.getId(), prismaUser.getEmail());

        Cookie cookie = new Cookie(appProperties.jwt().cookieName(), token);
        cookie.setHttpOnly(true);
        cookie.setSecure(appProperties.jwt().cookieSecure());
        cookie.setPath("/");
        cookie.setMaxAge((int) (appProperties.jwt().expirationMinutes() * 60));
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);

        response.sendRedirect(appProperties.frontendBaseUrl() + "/oauth2/redirect");
    }
}
