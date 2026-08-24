package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.activity.ActivityPublisher;
import com.richardbrenkus.hospitalshiftscheduler.activity.RequestMetadata;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ActivityType;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ReminderEmailOutboxStatus;
import com.richardbrenkus.hospitalshiftscheduler.entity.SendReminderTask;
import com.richardbrenkus.hospitalshiftscheduler.exception.PermanentEmailDeliveryException;
import com.richardbrenkus.hospitalshiftscheduler.exception.TransientEmailDeliveryException;
import com.richardbrenkus.hospitalshiftscheduler.repository.ReminderEmailOutboxRepository;
import com.richardbrenkus.hospitalshiftscheduler.repository.SendReminderTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderEmailOutboxProcessor {

    private static final List<ReminderEmailOutboxStatus> DISPATCHABLE_STATUSES = List.of(ReminderEmailOutboxStatus.PENDING, ReminderEmailOutboxStatus.FAILED);

    private static final String TRANSIENT_FAILURE_REASON = "Temporary reminder email delivery failure";
    private static final String PERMANENT_FAILURE_REASON = "Permanent reminder email delivery failure";
    private static final String UNEXPECTED_FAILURE_REASON = "Unexpected reminder email delivery failure";

    private final ReminderEmailOutboxRepository repository;
    private final ReminderEmailOutboxClaimService claimService;
    private final ReminderEmailOutboxCompletionService completionService;
    private final EmailReminderService emailReminderService;
    private final SendReminderTaskRepository sendReminderTaskRepository;
    private final ActivityPublisher activityPublisher;
    private final Clock applicationClock;

    @Value("${planned-tasks.outbox.batch-size:20}")
    private int batchSize;

    private final String workerId = UUID.randomUUID().toString();

    @Scheduled(fixedDelayString = "${planned-tasks.outbox.fixed-delay-ms:10000}")
    public void processPendingReminderJobs() {
        validateBatchSize();

        Instant now = Instant.now(applicationClock);

        List<Long> candidateIds = repository.findDispatchableIds(DISPATCHABLE_STATUSES, now, PageRequest.of(0, batchSize));

        for (Long candidateId : candidateIds) {
            try {
                processOne(candidateId);
            } catch (RuntimeException exception) {
                log.error("Unexpected failure while processing outbox job {}", candidateId, exception);
            }
        }
    }

    private void processOne(Long outboxId) {
        if (outboxId == null) {
            log.warn("Skipping null reminder outbox ID");
            return;
        }

        Instant claimTime = Instant.now(applicationClock);

        claimService.claim(outboxId, workerId, claimTime).ifPresent(this::processClaimedJob);
    }

    private void processClaimedJob(ReminderEmailOutboxClaimService.ClaimedReminderEmailJob job) {
        if (isReminderTaskInactive()) {
            cancelJobBecauseTaskInactive(job);
            return;
        }

        try {
            emailReminderService.sendShiftRequestReminderEmail(job.recipientEmail(), job.recipientDisplayName(), job.finalSubmissionDay(), job.idempotencyKey());
        } catch (TransientEmailDeliveryException exception) {
            handleTransientDeliveryFailure(job, exception);
            return;
        } catch (PermanentEmailDeliveryException exception) {
            handlePermanentDeliveryFailure(job, exception);
            return;
        } catch (RuntimeException exception) {
            /*
             * An exception outside the explicit delivery model is treated as
             * a programming/configuration defect and terminated immediately.
             * The exception itself is logged, but only a safe generic reason
             * is persisted.
             */
            handleUnexpectedDeliveryFailure(job, exception);
            return;
        }

        markSent(job);
    }

    private void markSent(ReminderEmailOutboxClaimService.ClaimedReminderEmailJob job) {
        try {
            boolean changed = completionService.markSent(job.outboxId(), job.claimToken(), Instant.now(applicationClock));

            if (changed) {
                publishSentActivity(job.outboxId());
            } else {
                log.warn("Reminder email for outbox job {} was accepted for delivery, but its claim token no longer owns the row.", job.outboxId());
            }
        } catch (RuntimeException completionException) {
            log.error("Reminder email for outbox job {} was accepted for delivery, but the job could not be marked SENT. The row will remain PROCESSING for stale-claim recovery.", job.outboxId(), completionException);
        }
    }

    private void handleTransientDeliveryFailure(ReminderEmailOutboxClaimService.ClaimedReminderEmailJob job, TransientEmailDeliveryException deliveryException) {
        log.error("Transient reminder email delivery failure for outbox job {}, recipient user {}, attempt {}", job.outboxId(), job.recipientUserId(), job.attemptNumber(), deliveryException);

        completeFailure(job, TRANSIENT_FAILURE_REASON, false);
    }

    private void handlePermanentDeliveryFailure(ReminderEmailOutboxClaimService.ClaimedReminderEmailJob job, PermanentEmailDeliveryException deliveryException) {
        log.error("Permanent reminder email delivery failure for outbox job {}, recipient user {}, attempt {}", job.outboxId(), job.recipientUserId(), job.attemptNumber(), deliveryException);

        completeFailure(job, PERMANENT_FAILURE_REASON, true);
    }

    private void handleUnexpectedDeliveryFailure(ReminderEmailOutboxClaimService.ClaimedReminderEmailJob job, RuntimeException deliveryException) {
        log.error("Unexpected reminder email delivery failure for outbox job {}, recipient user {}, attempt {}", job.outboxId(), job.recipientUserId(), job.attemptNumber(), deliveryException);

        completeFailure(job, UNEXPECTED_FAILURE_REASON, true);
    }

    private void completeFailure(ReminderEmailOutboxClaimService.ClaimedReminderEmailJob job, String safeFailureReason, boolean permanent) {
        try {
            ReminderEmailOutboxCompletionService.FailureCompletionResult result = permanent ? completionService.markPermanentFailure(job.outboxId(), job.claimToken(), Instant.now(applicationClock), safeFailureReason) : completionService.markTransientFailure(job.outboxId(), job.claimToken(), Instant.now(applicationClock), safeFailureReason);

            switch (result) {
                case RETRY_SCHEDULED -> publishFailedActivity(job.outboxId(), safeFailureReason, false);

                case DEAD -> publishFailedActivity(job.outboxId(), safeFailureReason, true);

                case NOT_CHANGED ->
                        log.warn("Delivery failed for outbox job {}, but its claim token no longer owns the row. The failure was not applied to a newer claim.", job.outboxId());
            }
        } catch (RuntimeException completionException) {
            log.error("Reminder email delivery failed for outbox job {}, and the job could not be completed as FAILED or DEAD", job.outboxId(), completionException);
        }
    }

    private void publishSentActivity(Long outboxId) {
        activityPublisher.publishSuccess(ActivityType.REMINDER_EMAIL_SENT, "ReminderEmailOutbox", outboxId.toString(), "Shift-request reminder email sent");
    }

    private void publishFailedActivity(Long outboxId, String safeFailureReason, boolean dead) {
        String description = dead ? "Shift-request reminder email moved to DEAD" : "Sending shift-request reminder email failed; retry scheduled";

        activityPublisher.publishFailure(ActivityType.REMINDER_EMAIL_FAILED, "ReminderEmailOutbox", outboxId.toString(), description, safeFailureReason, RequestMetadata.system());
    }

    private void validateBatchSize() {
        if (batchSize <= 0) {
            throw new IllegalStateException("planned-tasks.outbox.batch-size must be greater than zero");
        }
    }

    private boolean isReminderTaskInactive() {
        return sendReminderTaskRepository.findById(SendReminderTask.SINGLETON_ID).map(SendReminderTask::isActive).map(active -> !active).orElse(false);
    }

    private void cancelJobBecauseTaskInactive(ReminderEmailOutboxClaimService.ClaimedReminderEmailJob job) {
        try {
            boolean canceled = completionService.cancelClaimedJob(job.outboxId(), job.claimToken());

            if (canceled) {
                log.info("Reminder outbox job {} cancelled after admin disabled reminders; SMTP send skipped.", job.outboxId());
            } else {
                log.warn("Reminder outbox job {} could not be cancelled after admin disabled reminders; claim token no longer owns the row.", job.outboxId());
            }
        } catch (RuntimeException exception) {
            log.error("Reminder outbox job {} could not be cancelled after admin disabled reminders. The row will remain PROCESSING for stale-claim recovery.", job.outboxId(), exception);
        }
    }
}
