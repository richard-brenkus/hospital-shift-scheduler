package com.richardbrenkus.hospitalshiftscheduler.activity.kafka;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityKafkaPropertiesTest {

    @Test
    void shouldFallBackToDefaults_whenBootstrapAndTopicAndClientIdAreNull() {
        ActivityKafkaProperties properties = new ActivityKafkaProperties(false, null, null, null);

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.bootstrapServers()).isEqualTo("localhost:9092");
        assertThat(properties.topic()).isEqualTo("shift-scheduler.activity-events.v1");
        assertThat(properties.clientId()).isEqualTo("shift-scheduler-activity-producer");
    }

    @Test
    void shouldFallBackToDefaults_whenValuesAreBlank() {
        ActivityKafkaProperties properties = new ActivityKafkaProperties(true, "   ", "\t", "");

        assertThat(properties.bootstrapServers()).isEqualTo("localhost:9092");
        assertThat(properties.topic()).isEqualTo("shift-scheduler.activity-events.v1");
        assertThat(properties.clientId()).isEqualTo("shift-scheduler-activity-producer");
    }

    @Test
    void shouldPreserveExplicitConfiguredValues() {
        ActivityKafkaProperties properties = new ActivityKafkaProperties(true, "broker-1:19092,broker-2:19092", "custom-topic.v2", "explicit-client-id");

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.bootstrapServers()).isEqualTo("broker-1:19092,broker-2:19092");
        assertThat(properties.topic()).isEqualTo("custom-topic.v2");
        assertThat(properties.clientId()).isEqualTo("explicit-client-id");
    }
}
