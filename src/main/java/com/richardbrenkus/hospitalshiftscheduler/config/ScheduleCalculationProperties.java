package com.richardbrenkus.hospitalshiftscheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.schedule-calculation")
public record ScheduleCalculationProperties(
        int numberOfThreads,
        int attemptsPerThread,
        Duration timeout
) {
}
