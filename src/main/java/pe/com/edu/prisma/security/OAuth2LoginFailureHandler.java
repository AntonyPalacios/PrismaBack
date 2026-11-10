package pe.com.edu.prisma.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import pe.com.edu.prisma.config.AppProperties;

/**
 * A rejected Google login goes back to the frontend's login page with a
 * short error code, instead of Spring's default {@code /login?error} page
 * on the backend. Only two codes exist: {@code not_registered} (unknown
 * or inactive email — deliberately indistinguishable) and
 * {@code login_failed} (anything else, e.g. the user cancelled at Google).
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    private final AppProperties appProperties;

    public OAuth2LoginFailureHandler(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        String code = exception instanceof OAuth2AuthenticationException oauth
                && GoogleLoginService.NOT_REGISTERED.equals(oauth.getError().getErrorCode())
                ? GoogleLoginService.NOT_REGISTERED
                : "login_failed";
        log.warn("Google login rejected ({}): {}", code, exception.getMessage());
        response.sendRedirect(appProperties.frontendBaseUrl() + "/login?error=" + code);
    }
}
