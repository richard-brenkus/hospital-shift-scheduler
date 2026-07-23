package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ReminderEmailOutboxStatus;
import com.richardbrenkus.shiftschedulermodernized.entity.ReminderEmailOutbox;
import com.richardbrenkus.shiftschedulermodernized.repository.ReminderEmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReminderEmailOutboxClaimService {

    private final ReminderEmailOutboxRepository repository;

    @Transactional
    public Optional<ClaimedReminderEmailJob> claim(
            Long outboxId,
            String workerId,
            LocalDateTime now
    ) {
        validateArguments(outboxId, workerId, now);

        ReminderEmailOutbox outbox = repository
                .findByIdForUpdate(outboxId)
                .orElse(null);

        if (!isDispatchable(outbox, now)) {
            return Optional.empty();
        }

        String claimToken = UUID.randomUUID().toString();
        outbox.claim(workerId, claimToken, now);
        //repository.saveAndFlush(outbox);

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

    private boolean isDispatchable(ReminderEmailOutbox outbox, LocalDateTime now) {
        if (outbox == null) {
            return false;
        }

        boolean dispatchableStatus =
                outbox.getStatus() == ReminderEmailOutboxStatus.PENDING
                        || outbox.getStatus() == ReminderEmailOutboxStatus.FAILED;

        return dispatchableStatus
                && outbox.getNextAttemptAt() != null
                && !outbox.getNextAttemptAt().isAfter(now);
    }

    private void validateArguments(Long outboxId, String workerId, LocalDateTime now) {
        if (outboxId == null) {
            throw new IllegalArgumentException("outboxId must not be null");
        }
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId must not be blank");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
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
            if (claimToken == null || claimToken.isBlank()) {
                throw new IllegalArgumentException("claimToken must not be blank");
            }
            if (recipientEmail == null || recipientEmail.isBlank()) {
                throw new IllegalArgumentException("recipientEmail must not be blank");
            }

            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("idempotencyKey must not be blank");
            }

            Objects.requireNonNull(outboxId, "outboxId must not be null");
            Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
            Objects.requireNonNull(recipientDisplayName, "recipientDisplayName must not be null");
            Objects.requireNonNull(finalSubmissionDay, "finalSubmissionDay must not be null");
            if(attemptNumber <= 0) throw new IllegalArgumentException("attemptNumber must be greater than 0");
        }
    }
}