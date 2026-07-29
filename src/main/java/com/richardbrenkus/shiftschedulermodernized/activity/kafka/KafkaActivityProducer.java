package com.richardbrenkus.shiftschedulermodernized.activity.kafka;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "application.kafka.activity", name = "enabled", havingValue = "true")
public class KafkaActivityProducer implements ActivityKafkaProducer {

    private final ActivityKafkaMapper activityKafkaMapper;
    private final ActivityKafkaProperties activityKafkaProperties;
    private final KafkaTemplate<String, ActivityKafkaMessage> activityKafkaTemplate;

    public KafkaActivityProducer(ActivityKafkaMapper activityKafkaMapper, ActivityKafkaProperties activityKafkaProperties, @Qualifier("activityKafkaTemplate") KafkaTemplate<String, ActivityKafkaMessage> activityKafkaTemplate) {
        this.activityKafkaMapper = activityKafkaMapper;
        this.activityKafkaProperties = activityKafkaProperties;
        this.activityKafkaTemplate = activityKafkaTemplate;
    }

    @Override
    public void publish(ActivityEvent event) {
        ActivityKafkaMessage message = activityKafkaMapper.toMessage(event);
        String messageKey = createMessageKey(message);

        /*
         * send(...) is asynchronous. Failure is handled in the callback and is
         * deliberately not rethrown into the already-completed business flow.
         */
        activityKafkaTemplate.send(activityKafkaProperties.topic(), messageKey, message).whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Failed to publish activity event {} to Kafka topic {}", message.eventId(), activityKafkaProperties.topic(), throwable);
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("Published activity event {} to Kafka topic {}, partition {}, offset {}", message.eventId(), result.getRecordMetadata().topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }

    private String createMessageKey(ActivityKafkaMessage message) {
        UUID eventId = message.eventId();
        return eventId == null ? "" : eventId.toString();
    }
}