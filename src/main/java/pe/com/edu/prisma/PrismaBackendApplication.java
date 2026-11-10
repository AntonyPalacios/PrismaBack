package pe.com.edu.prisma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "auditingDateTimeProvider")
@ConfigurationPropertiesScan
public class PrismaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrismaBackendApplication.class, args);
    }
}
