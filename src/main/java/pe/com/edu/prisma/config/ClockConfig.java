package pe.com.edu.prisma.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "Today" for business rules (has this cycle started? is it closing early?)
 * is always the date in Lima, wherever the server runs. Rules ask this
 * clock instead of calling {@code LocalDate.now()} themselves.
 */
@Configuration
public class ClockConfig {

    public static final ZoneId ACADEMY_ZONE = ZoneId.of("America/Lima");

    @Bean
    public Clock clock() {
        return Clock.system(ACADEMY_ZONE);
    }
}
