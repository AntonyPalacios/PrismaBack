package pe.com.edu.prisma.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import pe.com.edu.prisma.config.ContainersConfig;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.UserRepository;

/**
 * What happens after Google says "this is X": only active, registered,
 * verified emails get through, and nobody is ever auto-created. Tested
 * without Google itself — PrismaOidcUserService only adapts the OIDC
 * profile into a call to GoogleLoginService.
 */
@SpringBootTest
@Import(ContainersConfig.class)
class GoogleLoginIT {

    @Autowired
    private GoogleLoginService googleLoginService;

    @Autowired
    private OAuth2LoginFailureHandler failureHandler;

    @Autowired
    private UserRepository userRepository;

    private User newUser(boolean active) {
        User user = new User();
        user.setEmail("google-" + UUID.randomUUID() + "@example.com");
        user.setFirstName("Test");
        user.setLastNamePat("Google");
        user.setActive(active);
        return userRepository.save(user);
    }

    @Test
    void registeredUserLogsInAndPhotoAndSubAreRefreshed() {
        User user = newUser(true);
        String sub = "google-sub-" + UUID.randomUUID();

        User loggedIn = googleLoginService.registerLogin(
                user.getEmail().toUpperCase(), true, sub, "https://photo.example/a.jpg");

        assertThat(loggedIn.getId()).isEqualTo(user.getId());
        User stored = userRepository.findById(user.getId()).orElseThrow();
        assertThat(stored.getPhotoUrl()).isEqualTo("https://photo.example/a.jpg");
        assertThat(stored.getGoogleSub()).isEqualTo(sub);
    }

    @Test
    void unknownEmailIsRejectedAndNoUserIsCreated() {
        long before = userRepository.count();

        assertThatThrownBy(() -> googleLoginService.registerLogin("nobody@example.com", true, "sub", null))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(ex -> ((OAuth2AuthenticationException) ex).getError().getErrorCode())
                .isEqualTo(GoogleLoginService.NOT_REGISTERED);
        assertThat(userRepository.count()).isEqualTo(before);
    }

    @Test
    void inactiveUserGetsTheSameRejectionAsAnUnknownOne() {
        User user = newUser(false);

        assertThatThrownBy(() -> googleLoginService.registerLogin(user.getEmail(), true, "sub", null))
                .extracting(ex -> ((OAuth2AuthenticationException) ex).getError().getErrorCode())
                .isEqualTo(GoogleLoginService.NOT_REGISTERED);
    }

    @Test
    void unverifiedGoogleEmailIsRejected() {
        User user = newUser(true);

        assertThatThrownBy(() -> googleLoginService.registerLogin(user.getEmail(), false, "sub", null))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void rejectedLoginRedirectsToTheFrontendLoginPage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationException notRegistered = catchNotRegistered();

        failureHandler.onAuthenticationFailure(new MockHttpServletRequest(), response, notRegistered);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?error=not_registered");
    }

    @Test
    void anyOtherLoginFailureRedirectsWithAGenericCode() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new BadCredentialsException("cancelled at Google"));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?error=login_failed");
    }

    private OAuth2AuthenticationException catchNotRegistered() {
        try {
            googleLoginService.registerLogin("nobody@example.com", true, "sub", null);
            throw new AssertionError("expected rejection");
        } catch (OAuth2AuthenticationException ex) {
            return ex;
        }
    }
}
