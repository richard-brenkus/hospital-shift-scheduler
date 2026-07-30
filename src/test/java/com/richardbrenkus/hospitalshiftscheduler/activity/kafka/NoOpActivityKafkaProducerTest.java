package com.richardbrenkus.hospitalshiftscheduler.activity.kafka;

import com.richardbrenkus.hospitalshiftscheduler.activity.ActivityEvent;
import com.richardbrenkus.hospitalshiftscheduler.activity.RequestMetadata;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ActivityType;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.Role;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class NoOpActivityKafkaProducerTest {

    private final NoOpActivityKafkaProducer producer = new NoOpActivityKafkaProducer();

    @Test
    void publish_shouldNeverInteractWithKafka() {
        // The no-op producer is the default implementation while Kafka is disabled.
        // Confirm it exposes the adapter contract without touching a KafkaTemplate.
        KafkaTemplate<?, ?> template = mock(KafkaTemplate.class);

        producer.publish(anyEvent());

        verifyNoInteractions(template);
    }

    @Test
    void publish_shouldNeverThrow() {
        assertThatCode(() -> producer.publish(anyEvent())).doesNotThrowAnyException();
    }

    private ActivityEvent anyEvent() {
        return ActivityEvent.success(Instant.parse("2026-08-05T08:00:00Z"), ActivityType.USER_CREATED, "alice", Role.ADMIN, "User", "1", "Created", new RequestMetadata("POST", "/x", "1.2.3.4"));
    }
}
