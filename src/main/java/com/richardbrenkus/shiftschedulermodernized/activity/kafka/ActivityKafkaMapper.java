package com.richardbrenkus.shiftschedulermodernized.activity.kafka;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityEvent;
import org.springframework.stereotype.Component;

@Component
public class ActivityKafkaMapper {

    public ActivityKafkaMessage toMessage(ActivityEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("ActivityEvent must not be null");
        }

        return new ActivityKafkaMessage(
                event.eventId(),
                event.occurredAt(),
                enumName(event.activityType()),
                nullToEmpty(event.actorUsername()),
                enumName(event.actorRole()),
                nullToEmpty(event.targetType()),
                nullToEmpty(event.targetId()),
                nullToEmpty(event.description()),
                event.successful(),
                nullToEmpty(event.failureReason()),
                event.requestMetadata().requestMethod(),
                event.requestMetadata().requestPath(),
                event.requestMetadata().clientIp()
        );
    }

    private String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}