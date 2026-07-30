package com.richardbrenkus.hospitalshiftscheduler.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ShiftTypeProperties.class, ScheduleCalculationProperties.class})
public class AppConfig {
}

