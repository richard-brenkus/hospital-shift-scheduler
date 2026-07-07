package com.richardbrenkus.shiftschedulermodernized.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Bean
    public ZoneId applicationZoneId(@Value("${app.time-zone:Europe/Prague}") String timeZone) {
        return ZoneId.of(timeZone);
    }

    @Bean
    public Clock applicationClock(ZoneId applicationZoneId) {
        return Clock.system(applicationZoneId);
    }
}

