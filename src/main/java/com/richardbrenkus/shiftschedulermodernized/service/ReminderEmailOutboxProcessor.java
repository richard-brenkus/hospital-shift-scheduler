package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.activity.RequestMetadata;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ReminderEmailOutboxStatus;
import com.richardbrenkus.shiftschedulermodernized.repository.ReminderEmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderEmailOutboxProcessor {

    private static final List<ReminderEmailOutboxStatus> DISPATCHABLE_STATUSES =
            List.of(
                    ReminderEmailOutboxStatus.PENDING,
                    ReminderEmailOutboxStatus.FAILED
            );

    private static final String SAFE_DELIVERY_FAILURE_REASON =
            "Reminder email delivery failed";

    private final ReminderEmailOutboxRepository repository;
    private final ReminderEmailOutboxClaimService claimService;
    private final ReminderEmailOutboxCompletionService completionService;
    private final EmailReminderService emailReminderService;
    private final ActivityPublisher activityPublisher;
    private final Clock applicationClock;

    @Value("${planned-tasks.outbox.batch-size:20}")
    private int batchSize;

    private final String workerId = UUID.randomUUID().toString();

    @Scheduled(fixedDelayString = "${planned-tasks.outbox.fixed-delay-ms:10000}")
    public void processPendingReminderJobs() {
        validateBatchSize();

        LocalDateTime now = LocalDateTime.now(applicationClock);
        List<Long> candidateIds = repository.findDispatchableIds(
                DISPATCHABLE_STATUSES,
                now,
                PageRequest.of(0, batchSize)
        );

        for (Long candidateId : candidateIds) {
            try {
                processOne(candidateId);
            } catch (RuntimeException exception) {
                log.error(
                        "Unexpected failure while processing outbox job {}",
                        candidateId,
                        exception
                );
            }
        }
    }

    private void processOne(Long outboxId) {
        if (outboxId == null) {
            log.warn("Skipping null reminder outbox ID");
            return;
        }

        LocalDateTime claimTime = LocalDateTime.now(applicationClock);

        claimService.claim(outboxId, workerId, claimTime)
                .ifPresent(this::processClaimedJob);
    }

    private void processClaimedJob(
            ReminderEmailOutboxClaimService.ClaimedReminderEmailJob job
    ) {
        try {
            emailReminderService.sendShiftRequestReminderEmail(
                    job.recipientEmail(),
                    job.recipientDisplayName(),
                    job.finalSubmissionDay(),
                    job.idempotencyKey()
            );
        } catch (RuntimeException deliveryException) {
            handleDeliveryFailure(job, deliveryException);
            return;
        }

        try {
            boolean changed = completionService.markSent(
                    job.outboxId(),
                    job.claimToken(),
                    LocalDateTime.now(applicationClock)
            );

            if (changed) {
                publishSentActivitySafely(job.outboxId());
            } else {
                log.warn(
                        "Reminder email for outbox job {} was accepted for delivery, "
                                + "but its claim token no longer owns the row.",
                        job.outboxId()
                );
            }
        } catch (RuntimeException completionException) {
            log.error(
                    "Reminder email for outbox job {} was accepted for delivery, "
                            + "but the job could not be marked SENT. The row will "
                            + "remain PROCESSING for stale-claim recovery.",
                    job.outboxId(),
                    completionException
            );
        }
    }

    private void handleDeliveryFailure(
            ReminderEmailOutboxClaimService.ClaimedReminderEmailJob job,
            RuntimeException deliveryException
    ) {
        log.error(
                "Reminder email delivery failed for outbox job {}, recipient user {}, attempt {}",
                job.outboxId(),
                job.recipientUserId(),
                job.attemptNumber(),
                deliveryException
        );

        try {
            boolean changed = completionService.markFailed(
                    job.outboxId(),
                    job.claimToken(),
                    LocalDateTime.now(applicationClock),
                    SAFE_DELIVERY_FAILURE_REASON
            );

            if (changed) {
                publishFailedActivitySafely(
                        job.outboxId(),
                        SAFE_DELIVERY_FAILURE_REASON
                );
            } else {
                log.warn(
                        "Delivery failed for outbox job {}, but its claim token no "
                                + "longer owns the row. The failure was not applied "
                                + "to a newer claim.",
                        job.outboxId()
                );
            }
        } catch (RuntimeException completionException) {
            log.error(
                    "Reminder email delivery failed for outbox job {}, and the job "
                            + "could not be marked FAILED",
                    job.outboxId(),
                    completionException
            );
        }
    }

    private void publishSentActivitySafely(Long outboxId) {
        try {
            activityPublisher.publishSuccess(
                    ActivityType.REMINDER_EMAIL_SENT,
                    "ReminderEmailOutbox",
                    outboxId.toString(),
                    "Shift-request reminder email sent"
            );
        } catch (RuntimeException activityException) {
            log.error(
                    "Outbox job {} was marked SENT, but its success activity could not be published",
                    outboxId,
                    activityException
            );
        }
    }

    private void publishFailedActivitySafely(
            Long outboxId,
            String safeFailureReason
    ) {
        try {
            activityPublisher.publishFailure(
                    ActivityType.REMINDER_EMAIL_FAILED,
                    "ReminderEmailOutbox",
                    outboxId.toString(),
                    "Sending shift-request reminder email failed",
                    safeFailureReason,
                    RequestMetadata.system()
            );
        } catch (RuntimeException activityException) {
            log.error(
                    "Outbox job {} was marked FAILED, but its failure activity could not be published",
                    outboxId,
                    activityException
            );
        }
    }

    private void validateBatchSize() {
        if (batchSize <= 0) {
            throw new IllegalStateException(
                    "planned-tasks.outbox.batch-size must be greater than zero"
            );
        }
    }
}