package pe.com.edu.prisma.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String frontendBaseUrl, Cors cors, Jwt jwt) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Jwt(String secret, long expirationMinutes, String cookieName, boolean cookieSecure) {
    }
}
