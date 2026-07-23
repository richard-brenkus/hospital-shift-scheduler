package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.entity.ReminderEmailOutbox;
import com.richardbrenkus.shiftschedulermodernized.repository.ReminderEmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReminderEmailOutboxCompletionService {

    private static final int MAXIMUM_BACKOFF_ATTEMPT = 5;

    private final ReminderEmailOutboxRepository repository;

    @Transactional
    public boolean markSent(Long outboxId, String claimToken, LocalDateTime now) {
        validateRequiredArguments(outboxId, claimToken, now);

        ReminderEmailOutbox outbox = repository
                .findByIdForUpdate(outboxId)
                .orElse(null);

        if (outbox == null || !outbox.isOwnedByClaim(claimToken)) {
            return false;
        }

        outbox.markSent(claimToken, now);
        //repository.saveAndFlush(outbox);
        return true;
    }

    @Transactional
    public boolean markFailed(
            Long outboxId,
            String claimToken,
            LocalDateTime now,
            String safeFailureReason
    ) {
        validateRequiredArguments(outboxId, claimToken, now);

        ReminderEmailOutbox outbox = repository
                .findByIdForUpdate(outboxId)
                .orElse(null);

        if (outbox == null || !outbox.isOwnedByClaim(claimToken)) {
            return false;
        }

        String normalizedFailureReason = normalizeFailureReason(safeFailureReason);
        LocalDateTime retryAt = calculateRetryAt(now, outbox.getAttemptCount());

        outbox.markFailed(claimToken, normalizedFailureReason, retryAt);
        //repository.saveAndFlush(outbox);
        return true;
    }

    private LocalDateTime calculateRetryAt(LocalDateTime now, int attemptCount) {
        int boundedAttempt = Math.clamp(attemptCount, 1, MAXIMUM_BACKOFF_ATTEMPT);
        long delayMinutes = 1L << (boundedAttempt - 1);
        return now.plusMinutes(delayMinutes);
    }

    private void validateRequiredArguments(
            Long outboxId,
            String claimToken,
            LocalDateTime now
    ) {
        if (outboxId == null) {
            throw new IllegalArgumentException("outboxId must not be null");
        }
        if (claimToken == null || claimToken.isBlank()) {
            throw new IllegalArgumentException("claimToken must not be blank");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
    }

    private String normalizeFailureReason(String safeFailureReason) {
        if (safeFailureReason == null || safeFailureReason.isBlank()) {
            return "Reminder email delivery failed";
        }
        return safeFailureReason.trim();
    }
}