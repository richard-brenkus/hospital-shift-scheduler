package com.richardbrenkus.shiftschedulermodernized.kafka;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityKafkaPublisher {

    /*private final KafkaTemplate<String, ActivityKafkaMessage> kafkaTemplate;

    @EventListener
    public void publish(ActivityEvent event) {
        ActivityKafkaMessage message =
                ActivityKafkaMessage.from(event);

        kafkaTemplate.send(
                "user-activity",
                event.eventId().toString(),
                message
        );
    }*/
}
