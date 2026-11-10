package pe.com.edu.prisma.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.com.edu.prisma.security.ApiAuthenticationEntryPoint;
import pe.com.edu.prisma.security.CsrfCookieFilter;
import pe.com.edu.prisma.security.JwtCookieAuthenticationFilter;
import pe.com.edu.prisma.security.JwtService;
import pe.com.edu.prisma.security.OAuth2LoginFailureHandler;
import pe.com.edu.prisma.security.OAuth2LoginSuccessHandler;
import pe.com.edu.prisma.security.PrismaOidcUserService;
import pe.com.edu.prisma.users.UserRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // required for the @PreAuthorize("hasRole('ADMIN')") checks on controllers
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AppProperties appProperties,
            PrismaOidcUserService prismaOidcUserService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            OAuth2LoginFailureHandler oAuth2LoginFailureHandler,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
            JwtService jwtService,
            UserRepository userRepository) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource(appProperties)))
                // Auth rides on a cookie instead of a bearer header, so CSRF
                // protection is required (a browser happily attaches cookies to
                // a forged request). spa(): the token lives in a JS-readable
                // XSRF-TOKEN cookie that the frontend echoes as X-XSRF-TOKEN.
                .csrf(csrf -> csrf.spa())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/oauth2/**", "/login/**",
                                "/actuator/health",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().authenticated())
                // The API answers 401 (Problem Details) instead of redirecting
                // to Google; everything else keeps the default redirect.
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        apiAuthenticationEntryPoint,
                        PathPatternRequestMatcher.pathPattern("/api/**")))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(info -> info.oidcUserService(prismaOidcUserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler))
                // POST only (CSRF-protected), idempotent: always 204, with or
                // without a session, and always expires the JWT cookie.
                .logout(logout -> logout
                        .logoutRequestMatcher(
                                PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/auth/logout"))
                        .deleteCookies(appProperties.jwt().cookieName())
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterBefore(
                        new JwtCookieAuthenticationFilter(jwtService, userRepository, appProperties),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(appProperties.cors().allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // the auth cookie and the CSRF cookie both need to travel with the request
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
