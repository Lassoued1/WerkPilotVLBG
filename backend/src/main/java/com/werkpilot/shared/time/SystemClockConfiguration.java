package com.werkpilot.shared.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
