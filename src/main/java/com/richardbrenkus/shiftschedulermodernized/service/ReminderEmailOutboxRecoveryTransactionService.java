package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ReminderEmailOutboxStatus;
import com.richardbrenkus.shiftschedulermodernized.entity.ReminderEmailOutbox;
import com.richardbrenkus.shiftschedulermodernized.repository.ReminderEmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReminderEmailOutboxRecoveryTransactionService {

    private static final String STALE_CLAIM_REASON = "Processing claim expired";

    private final ReminderEmailOutboxRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseStaleClaim(Long outboxId, Instant staleBefore, Instant retryAt) {

        validateArguments(outboxId, staleBefore, retryAt);

        ReminderEmailOutbox outbox = repository.findByIdForUpdate(outboxId).orElse(null);

        if (!isStillStale(outbox, staleBefore)) {
            return false;
        }

        outbox.releaseStaleClaim(STALE_CLAIM_REASON, retryAt);

        return true;
    }

    private boolean isStillStale(ReminderEmailOutbox outbox, Instant staleBefore) {
        return outbox != null && outbox.getStatus() == ReminderEmailOutboxStatus.PROCESSING && outbox.getClaimedAt() != null && !outbox.getClaimedAt().isAfter(staleBefore);
    }

    private void validateArguments(Long outboxId, Instant staleBefore, Instant retryAt) {
        if (outboxId == null) {
            throw new IllegalArgumentException("outboxId must not be null");
        }

        if (staleBefore == null) {
            throw new IllegalArgumentException("staleBefore must not be null");
        }

        if (retryAt == null) {
            throw new IllegalArgumentException("retryAt must not be null");
        }
    }
}