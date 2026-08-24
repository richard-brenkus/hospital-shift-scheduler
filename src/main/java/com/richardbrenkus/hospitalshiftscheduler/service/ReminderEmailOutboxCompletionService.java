package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.entity.ReminderEmailOutbox;
import com.richardbrenkus.hospitalshiftscheduler.repository.ReminderEmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReminderEmailOutboxCompletionService {

    private static final int MAXIMUM_BACKOFF_ATTEMPT = 5;

    private final ReminderEmailOutboxRepository repository;

    @Value("${planned-tasks.outbox.maximum-attempts:5}")
    private int maximumAttempts;

    @Transactional
    public boolean markSent(Long outboxId, String claimToken, Instant now) {
        validateRequiredArguments(outboxId, claimToken, now);

        ReminderEmailOutbox outbox = repository.findByIdForUpdate(outboxId).orElse(null);

        if (outbox == null || !outbox.isOwnedByClaim(claimToken)) {
            return false;
        }

        outbox.markSent(claimToken, now);
        repository.saveAndFlush(outbox);

        return true;
    }

    @Transactional
    public FailureCompletionResult markTransientFailure(Long outboxId, String claimToken, Instant now, String safeFailureReason) {
        validateRequiredArguments(outboxId, claimToken, now);
        validateMaximumAttempts();

        ReminderEmailOutbox outbox = repository.findByIdForUpdate(outboxId).orElse(null);

        if (outbox == null || !outbox.isOwnedByClaim(claimToken)) {
            return FailureCompletionResult.NOT_CHANGED;
        }

        String normalizedFailureReason = normalizeFailureReason(safeFailureReason);

        if (outbox.getAttemptCount() >= maximumAttempts) {
            outbox.markDead(claimToken, normalizedFailureReason, now);
            repository.saveAndFlush(outbox);

            return FailureCompletionResult.DEAD;
        }

        Instant retryAt = calculateRetryAt(now, outbox.getAttemptCount());

        outbox.markFailed(claimToken, normalizedFailureReason, retryAt);
        repository.saveAndFlush(outbox);

        return FailureCompletionResult.RETRY_SCHEDULED;
    }

    /**
     * Deletes a currently-claimed outbox row without attempting delivery.
     * Callers use this when policy (for example, the admin disabling reminders)
     * dictates that no further SMTP send should happen for this row.
     */
    @Transactional
    public boolean cancelClaimedJob(Long outboxId, String claimToken) {
        if (outboxId == null) {
            throw new IllegalArgumentException("outboxId must not be null");
        }

        if (claimToken == null || claimToken.isBlank()) {
            throw new IllegalArgumentException("claimToken must not be blank");
        }

        ReminderEmailOutbox outbox = repository.findByIdForUpdate(outboxId).orElse(null);

        if (outbox == null || !outbox.isOwnedByClaim(claimToken)) {
            return false;
        }

        repository.delete(outbox);
        repository.flush();

        return true;
    }

    @Transactional
    public FailureCompletionResult markPermanentFailure(Long outboxId, String claimToken, Instant now, String safeFailureReason) {
        validateRequiredArguments(outboxId, claimToken, now);

        ReminderEmailOutbox outbox = repository.findByIdForUpdate(outboxId).orElse(null);

        if (outbox == null || !outbox.isOwnedByClaim(claimToken)) {
            return FailureCompletionResult.NOT_CHANGED;
        }

        outbox.markDead(claimToken, normalizeFailureReason(safeFailureReason), now);
        repository.saveAndFlush(outbox);

        return FailureCompletionResult.DEAD;
    }

    private Instant calculateRetryAt(Instant now, int attemptCount) {
        int boundedAttempt = Math.clamp(attemptCount, 1, MAXIMUM_BACKOFF_ATTEMPT);
        long delayMinutes = 1L << (boundedAttempt - 1);

        return now.plusSeconds(Math.multiplyExact(60, delayMinutes));
    }

    private void validateRequiredArguments(Long outboxId, String claimToken, Instant now) {
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

    private void validateMaximumAttempts() {
        if (maximumAttempts <= 0) {
            throw new IllegalStateException("planned-tasks.outbox.maximum-attempts must be greater than zero");
        }
    }

    private String normalizeFailureReason(String safeFailureReason) {
        if (safeFailureReason == null || safeFailureReason.isBlank()) {
            return "Reminder email delivery failed";
        }

        return safeFailureReason.trim();
    }

    public enum FailureCompletionResult {
        NOT_CHANGED, RETRY_SCHEDULED, DEAD
    }
}
