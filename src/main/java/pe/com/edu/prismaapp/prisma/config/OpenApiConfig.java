package pe.com.edu.prismaapp.prisma.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // Opcional, para aplicar a nivel de clase/método
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Sistema Prisma",
                version = "1.0",
                description = "Documentación de la API para el proyecto Sistema Prisma",
                contact = @Contact(
                        name = "Antony Palacios",
                        email = "apalaciosc0109@gmail.com"
                )
        ),
        security = { // Aplica la seguridad globalmente a toda la API por defecto
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth", // Nombre del esquema de seguridad (usado en @SecurityRequirement)
        type = SecuritySchemeType.HTTP, // Tipo de esquema HTTP
        scheme = "bearer", // Esquema Bearer
        bearerFormat = "JWT", // Formato del token (opcional, para documentación)
        description = "Introduce tu token JWT aquí para autorizar las solicitudes"
)
public class OpenApiConfig {
    // No necesitas lógica aquí, las anotaciones hacen el trabajo
}