package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ReminderEmailOutboxStatus;
import com.richardbrenkus.shiftschedulermodernized.entity.ReminderEmailOutbox;
import com.richardbrenkus.shiftschedulermodernized.repository.ReminderEmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReminderEmailOutboxClaimService {

    private static final String MAXIMUM_ATTEMPTS_REACHED_REASON =
            "Maximum reminder email delivery attempts reached";

    private final ReminderEmailOutboxRepository repository;

    @Value("${planned-tasks.outbox.maximum-attempts:5}")
    private int maximumAttempts;

    @Transactional
    public Optional<ClaimedReminderEmailJob> claim(
            Long outboxId,
            String workerId,
            LocalDateTime now
    ) {
        validateArguments(outboxId, workerId, now);
        validateMaximumAttempts();

        ReminderEmailOutbox outbox = repository
                .findByIdForUpdate(outboxId)
                .orElse(null);

        if (!isDispatchable(outbox, now)) {
            return Optional.empty();
        }

        /*
         * Normally the completion service moves the row to DEAD when the
         * configured limit is reached. This additional guard handles rows
         * released by stale-claim recovery and already-corrupted old rows.
         */
        if (outbox.getAttemptCount() >= maximumAttempts) {
            outbox.markDeadFromDispatchableState(
                    MAXIMUM_ATTEMPTS_REACHED_REASON,
                    now
            );
            repository.saveAndFlush(outbox);
            return Optional.empty();
        }

        String claimToken = UUID.randomUUID().toString();
        outbox.claim(workerId, claimToken, now);
        repository.saveAndFlush(outbox);

        return Optional.of(new ClaimedReminderEmailJob(
                outbox.getId(),
                claimToken,
                outbox.getRecipientUserId(),
                outbox.getRecipientEmail(),
                outbox.getRecipientDisplayName(),
                outbox.getFinalSubmissionDay(),
                outbox.getIdempotencyKey(),
                outbox.getAttemptCount()
        ));
    }

    private boolean isDispatchable(
            ReminderEmailOutbox outbox,
            LocalDateTime now
    ) {
        if (outbox == null) {
            return false;
        }

        boolean dispatchableStatus =
                outbox.getStatus() == ReminderEmailOutboxStatus.PENDING
                        || outbox.getStatus()
                        == ReminderEmailOutboxStatus.FAILED;

        return dispatchableStatus
                && outbox.getNextAttemptAt() != null
                && !outbox.getNextAttemptAt().isAfter(now);
    }

    private void validateArguments(
            Long outboxId,
            String workerId,
            LocalDateTime now
    ) {
        if (outboxId == null) {
            throw new IllegalArgumentException(
                    "outboxId must not be null"
            );
        }

        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException(
                    "workerId must not be blank"
            );
        }

        if (now == null) {
            throw new IllegalArgumentException(
                    "now must not be null"
            );
        }
    }

    private void validateMaximumAttempts() {
        if (maximumAttempts <= 0) {
            throw new IllegalStateException(
                    "planned-tasks.outbox.maximum-attempts "
                            + "must be greater than zero"
            );
        }
    }

    public record ClaimedReminderEmailJob(
            Long outboxId,
            String claimToken,
            Long recipientUserId,
            String recipientEmail,
            String recipientDisplayName,
            LocalDate finalSubmissionDay,
            String idempotencyKey,
            int attemptNumber
    ) {
        public ClaimedReminderEmailJob {
            if (outboxId == null) {
                throw new IllegalArgumentException(
                        "outboxId must not be null"
                );
            }

            if (claimToken == null || claimToken.isBlank()) {
                throw new IllegalArgumentException(
                        "claimToken must not be blank"
                );
            }

            if (recipientUserId == null) {
                throw new IllegalArgumentException(
                        "recipientUserId must not be null"
                );
            }

            if (recipientEmail == null || recipientEmail.isBlank()) {
                throw new IllegalArgumentException(
                        "recipientEmail must not be blank"
                );
            }

            if (finalSubmissionDay == null) {
                throw new IllegalArgumentException(
                        "finalSubmissionDay must not be null"
                );
            }

            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new IllegalArgumentException(
                        "idempotencyKey must not be blank"
                );
            }

            if (attemptNumber <= 0) {
                throw new IllegalArgumentException(
                        "attemptNumber must be greater than zero"
                );
            }
        }
    }
}
