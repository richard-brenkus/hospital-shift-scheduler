package com.richardbrenkus.shiftschedulermodernized.activity.kafka;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityEvent;
import com.richardbrenkus.shiftschedulermodernized.activity.RequestMetadata;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ActivityKafkaPublisherTest {

    @Mock
    private ActivityKafkaProducer producer;

    @InjectMocks
    private ActivityKafkaPublisher publisher;

    @Test
    void publishToKafka_shouldDelegateToProducer() {
        ActivityEvent event = ActivityEvent.success(
                Instant.parse("2026-08-05T08:00:00Z"),
                ActivityType.USER_CREATED,
                "alice",
                Role.ADMIN,
                "User",
                "1",
                "Created",
                new RequestMetadata("POST", "/x", "1.2.3.4")
        );

        publisher.publishToKafka(event);

        verify(producer).publish(event);
        verifyNoMoreInteractions(producer);
    }
}
