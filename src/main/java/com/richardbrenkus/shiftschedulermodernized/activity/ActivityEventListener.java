package com.richardbrenkus.shiftschedulermodernized.activity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityEventListener {

    private final ActivityLogWriter activityLogWriter;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(ActivityEvent event) {
        try {
            activityLogWriter.persist(event);
        } catch (RuntimeException exception) {
            log.error("Could not persist activity event of type {} for target {}:{}", event.activityType(), event.targetType(), event.targetId(), exception);
        }
    }
}