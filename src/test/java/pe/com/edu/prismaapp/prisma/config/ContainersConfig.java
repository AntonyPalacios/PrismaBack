package pe.com.edu.prismaapp.prisma.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Bean
    @ServiceConnection // <- La magia: inyecta URL, user y password a Spring automáticamente
    public MySQLContainer<?> mysqlContainer() {
        // AQUÍ es donde defines la imagen que coincide con tu driver 9.1.0
        return new MySQLContainer<>("mysql:9.1.0");
    }
}