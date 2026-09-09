package pe.com.edu.prisma.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import pe.com.edu.prisma.config.AppProperties;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(AppProperties appProperties) {
        this.key = Keys.hmacShaKeyFor(appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = appProperties.jwt().expirationMinutes();
    }

    public String issueToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key)
                .compact();
    }

    /** Returns the app_user id embedded in the token, or empty if it's missing/expired/tampered. */
    public java.util.Optional<Long> parseUserId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Number uid = claims.get("uid", Number.class);
            return java.util.Optional.ofNullable(uid).map(Number::longValue);
        } catch (Exception ex) {
            return java.util.Optional.empty();
        }
    }
}
