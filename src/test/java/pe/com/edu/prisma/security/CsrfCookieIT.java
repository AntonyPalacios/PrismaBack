package pe.com.edu.prisma.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import pe.com.edu.prisma.config.ContainersConfig;

/**
 * Over a real HTTP port, not MockMvc: spring-security-test swaps the CSRF
 * token repository inside MockMvc, so the cookie the browser actually
 * receives can only be observed against a running server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ContainersConfig.class)
class CsrfCookieIT {

    @LocalServerPort
    private int port;

    @Test
    void spaReceivesAJavascriptReadableCsrfCookieOnItsFirstRequest() throws Exception {
        HttpResponse<Void> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/me")).GET().build(),
                HttpResponse.BodyHandlers.discarding());

        List<String> csrfCookies = response.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("XSRF-TOKEN="))
                .toList();

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(csrfCookies).hasSize(1);
        assertThat(csrfCookies.getFirst()).doesNotContainIgnoringCase("HttpOnly");
    }
}
