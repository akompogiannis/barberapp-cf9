package gr.aueb.cf.barberapp.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// The application clock, injected everywhere rather than calling LocalDateTime.now() inline.
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
