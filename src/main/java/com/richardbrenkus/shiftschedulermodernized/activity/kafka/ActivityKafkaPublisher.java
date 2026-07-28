package com.richardbrenkus.shiftschedulermodernized.activity.kafka;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Subscribes the Kafka outbound adapter to the existing ActivityEvent flow.
 * AFTER_COMMIT prevents Kafka publication for a database transaction that
 * ultimately rolls back.
 * fallbackExecution=true also supports events that the existing activity
 * infrastructure publishes only after the transaction has already completed.
 */
@Component
@RequiredArgsConstructor
public class ActivityKafkaPublisher {

    private final ActivityKafkaProducer activityKafkaProducer;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void publishToKafka(ActivityEvent event) {
        activityKafkaProducer.publish(event);
    }
}
