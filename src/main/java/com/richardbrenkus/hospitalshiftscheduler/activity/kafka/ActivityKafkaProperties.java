package com.richardbrenkus.hospitalshiftscheduler.activity.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.kafka.activity")
public record ActivityKafkaProperties(boolean enabled, String bootstrapServers, String topic, String clientId) {

    public ActivityKafkaProperties {
        bootstrapServers = defaultIfBlank(bootstrapServers, "localhost:9092");
        topic = defaultIfBlank(topic, "shift-scheduler.activity-events.v1");
        clientId = defaultIfBlank(clientId, "shift-scheduler-activity-producer");
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}