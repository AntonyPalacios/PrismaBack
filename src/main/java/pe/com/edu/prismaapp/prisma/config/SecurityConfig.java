package pe.com.edu.prismaapp.prisma.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pe.com.edu.prismaapp.prisma.auth.CustomOAuth2UserService;
import pe.com.edu.prismaapp.prisma.auth.CustomUserDetailsService;
import pe.com.edu.prismaapp.prisma.auth.JwtRequestFilter;
import pe.com.edu.prismaapp.prisma.auth.JwtUtil;
import pe.com.edu.prismaapp.prisma.repositories.UserRepository;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Habilita @PreAuthorize en métodos
@EnableWebMvc // Habilita la configuración MVC de Spring para CORS global
public class SecurityConfig implements WebMvcConfigurer { // Implementa WebMvcConfigurer

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Value("${cors.allowedOrigins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Necesario aunque no uses password para el login normal
    }

    @Bean
    public AuthenticationManager authenticationManager(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Deshabilita CSRF para APIs sin estado
                 .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS manejado por WebMvcConfigurer
                .authorizeHttpRequests(authorize -> authorize
                        // Endpoints públicos (login, OAuth2 redirecciones, errores)
                        .requestMatchers("/auth/**", "/login/**", "/oauth2/**", "/error",
                                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/swagger-resources").permitAll()
                        // Ejemplo de protección por rol (descomentar y ajustar según tus necesidades)
                        // .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // .requestMatchers("/api/tutor/**").hasAnyRole("ADMIN", "TUTOR")
                        .anyRequest().authenticated() // Todas las demás peticiones requieren autenticación
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService) // Servicio para procesar la información del usuario de OAuth2
                        )
                        .successHandler((request, response, authentication) -> {
                            // Manejador de éxito para OAuth2
                            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                                // --- INICIO DE LA LÓGICA DE ACTUALIZACIÓN ---

                                // Obtenemos los atributos directamente del token de autenticación
                                String email = oauthToken.getPrincipal().getAttribute("email");
                                String name = oauthToken.getPrincipal().getAttribute("name");
                                String picture = oauthToken.getPrincipal().getAttribute("picture");
                                String provider = oauthToken.getAuthorizedClientRegistrationId(); // "google"
                                String providerId = oauthToken.getPrincipal().getName(); // ID único de Google

                                // Buscamos al usuario existente y actualizamos sus datos
                                // Esta es la misma lógica que tenías en CustomOAuth2UserService
                                userRepository.findByEmail(email).ifPresent(user -> {
                                    user.setName(name);
                                    user.setPicture(picture);
                                    user.setProvider(provider);
                                    user.setProviderId(providerId);
                                    userRepository.save(user); // Guardamos los cambios
                                });

                                // --- FIN DE LA LÓGICA DE ACTUALIZACIÓN ---
                                // Carga UserDetails para obtener los roles y generar el JWT
                                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
                                String jwt = jwtUtil.generateToken(userDetails); // Genera JWT con roles
                                // Redirige al frontend con el JWT como parámetro de URL
                                response.sendRedirect(allowedOrigins + "/oauth2/redirect?token=" + jwt);
                            } else {
                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Tipo de autenticación no soportado");
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            // Manejador de fallo para OAuth2
                            response.sendRedirect(allowedOrigins + "/login?error=" + exception.getMessage());
                        })
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // No usar sesiones HTTP (para APIs REST)
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class); // Añade el filtro JWT

        return http.build();
    }

    // Configuración CORS global usando WebMvcConfigurer
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**") // Aplica CORS a todas las rutas
//                .allowedOrigins(allowedOrigins) // Origen permitido desde application.yml
//                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // Métodos permitidos
//                .allowedHeaders("*") // Headers permitidos
//                .allowCredentials(true) // Permite credenciales (cookies, headers de auth)
//                .maxAge(3600); // Tiempo máximo en caché para preflight requests (en segundos)
//    }

     @Bean // Este bean ya no es necesario si CORS se maneja con WebMvcConfigurer
     public CorsConfigurationSource corsConfigurationSource() {
         CorsConfiguration configuration = new CorsConfiguration();
         configuration.setAllowedOrigins(List.of(allowedOrigins));
         configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
         configuration.setAllowedHeaders(List.of("*"));
         configuration.setAllowCredentials(true);
         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
         source.registerCorsConfiguration("/**", configuration);
         return source;
     }
}
