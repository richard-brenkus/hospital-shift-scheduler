package com.richardbrenkus.shiftschedulermodernized.activity.kafka;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default implementation used while Kafka integration is disabled.
 * It intentionally performs no logging for every event, because doing so would
 * create unnecessary noise in an application that legitimately runs without
 * Kafka.
 */
@Component
@Slf4j
@ConditionalOnProperty(
        prefix = "application.kafka.activity",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class NoOpActivityKafkaProducer implements ActivityKafkaProducer {

    @Override
    public void publish(ActivityEvent event) {
        // Kafka publication is intentionally disabled.
        log.debug("Activity event published: {}", event);
    }
}