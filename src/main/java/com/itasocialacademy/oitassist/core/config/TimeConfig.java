package com.itasocialacademy.oitassist.core.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {
    @Bean
    public Clock kyivClock() {
        return Clock.system(ZoneId.of("Europe/Kiev"));
    }
}
