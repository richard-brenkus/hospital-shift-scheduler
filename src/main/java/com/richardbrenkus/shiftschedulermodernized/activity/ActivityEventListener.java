package com.richardbrenkus.shiftschedulermodernized.activity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ActivityEventListener {

    private final ActivityLogWriter activityLogWriter;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ActivityEvent event) {
        activityLogWriter.write(event);
    }
}
