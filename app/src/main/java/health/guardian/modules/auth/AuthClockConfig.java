package health.guardian.modules.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AuthClockConfig {

    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }
}
