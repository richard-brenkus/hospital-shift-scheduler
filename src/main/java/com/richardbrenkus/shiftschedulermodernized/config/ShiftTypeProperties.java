package com.richardbrenkus.shiftschedulermodernized.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.shift-types")
public record ShiftTypeProperties(int count) {
}
