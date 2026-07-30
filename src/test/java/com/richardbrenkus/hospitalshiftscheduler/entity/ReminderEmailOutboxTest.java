package com.richardbrenkus.hospitalshiftscheduler.entity;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.ReminderEmailOutboxStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReminderEmailOutboxTest {

    private static final Instant NOW = Instant.parse("2026-08-05T08:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-08-20T21:59:59Z");
    private static final LocalDate FINAL_DAY = LocalDate.of(2026, 8, 20);
    private static final String WORKER = "worker-1";
    private static final String CLAIM_TOKEN = "claim-1";

    @Test
    void pending_shouldInitialiseInPendingStateWithZeroAttempts() {
        ReminderEmailOutbox job = pendingJob();

        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.PENDING);
        assertThat(job.getAttemptCount()).isZero();
        assertThat(job.getNextAttemptAt()).isEqualTo(NOW);
        assertThat(job.getCreatedAt()).isEqualTo(NOW);
        assertThat(job.getEventId()).isNotBlank();
        assertThat(job.getIdempotencyKey())
                .startsWith("shift-reminder:1:99:")
                .endsWith(job.getEventId());
        assertThat(job.getSentAt()).isNull();
        assertThat(job.getDeadAt()).isNull();
        assertThat(job.getClaimedAt()).isNull();
        assertThat(job.getClaimedBy()).isNull();
        assertThat(job.getClaimToken()).isNull();
    }

    @Test
    void pending_shouldRejectScheduleAfterDeadline() {
        assertThatThrownBy(() -> ReminderEmailOutbox.pending(
                1L,
                DEADLINE.plusSeconds(1),
                FINAL_DAY,
                DEADLINE,
                99L,
                "recipient@example.test",
                "Recipient",
                NOW
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pending_shouldRejectBlankRecipientEmail() {
        assertThatThrownBy(() -> ReminderEmailOutbox.pending(
                1L, NOW, FINAL_DAY, DEADLINE, 99L, " ",
                "Recipient", NOW
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void claim_shouldTransitionToProcessingAndIncrementAttemptCount() {
        ReminderEmailOutbox job = pendingJob();

        job.claim(WORKER, CLAIM_TOKEN, NOW);

        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.PROCESSING);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getClaimedBy()).isEqualTo(WORKER);
        assertThat(job.getClaimToken()).isEqualTo(CLAIM_TOKEN);
        assertThat(job.getClaimedAt()).isEqualTo(NOW);
        assertThat(job.isOwnedByClaim(CLAIM_TOKEN)).isTrue();
        assertThat(job.isOwnedByClaim("other-token")).isFalse();
    }

    @Test
    void claim_shouldRejectClaimTimeBeforeNextAttempt() {
        ReminderEmailOutbox job = pendingJob();

        assertThatThrownBy(() -> job.claim(WORKER, CLAIM_TOKEN, NOW.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void claim_shouldRejectClaimWhenStatusIsAlreadyProcessing() {
        ReminderEmailOutbox job = pendingJob();
        job.claim(WORKER, CLAIM_TOKEN, NOW);

        assertThatThrownBy(() -> job.claim(WORKER, "token-2", NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void claim_shouldRejectClaimWhenStatusIsSent() {
        ReminderEmailOutbox job = pendingJob();
        job.claim(WORKER, CLAIM_TOKEN, NOW);
        job.markSent(CLAIM_TOKEN, NOW.plusSeconds(1));

        assertThatThrownBy(() -> job.claim(WORKER, "token-2", NOW.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markSent_shouldTransitionToSentAndClearClaim() {
        ReminderEmailOutbox job = claimed();
        Instant sentAt = NOW.plusSeconds(5);

        job.markSent(CLAIM_TOKEN, sentAt);

        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.SENT);
        assertThat(job.getSentAt()).isEqualTo(sentAt);
        assertThat(job.getDeadAt()).isNull();
        assertThat(job.getClaimToken()).isNull();
        assertThat(job.getClaimedAt()).isNull();
        assertThat(job.getClaimedBy()).isNull();
        assertThat(job.getLastFailureReason()).isNull();
    }

    @Test
    void markSent_shouldRejectMismatchedClaimToken() {
        ReminderEmailOutbox job = claimed();

        assertThatThrownBy(() -> job.markSent("wrong-token", NOW.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.PROCESSING);
    }

    @Test
    void markFailed_shouldMoveJobBackToFailedAndScheduleRetry() {
        ReminderEmailOutbox job = claimed();
        Instant retryAt = NOW.plusSeconds(60);

        job.markFailed(CLAIM_TOKEN, "SMTP timeout", retryAt);

        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.FAILED);
        assertThat(job.getNextAttemptAt()).isEqualTo(retryAt);
        assertThat(job.getLastFailureReason()).isEqualTo("SMTP timeout");
        assertThat(job.getClaimToken()).isNull();
        assertThat(job.getSentAt()).isNull();
        assertThat(job.getDeadAt()).isNull();
    }

    @Test
    void markFailed_shouldSubstituteDefaultReasonWhenBlankProvided() {
        ReminderEmailOutbox job = claimed();

        job.markFailed(CLAIM_TOKEN, "   ", NOW.plusSeconds(60));

        assertThat(job.getLastFailureReason())
                .isEqualTo("Reminder email delivery failed");
    }

    @Test
    void markDead_shouldPreventFurtherRetries() {
        ReminderEmailOutbox job = claimed();

        job.markDead(CLAIM_TOKEN, "permanent bounce", NOW.plusSeconds(1));

        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.DEAD);
        assertThat(job.getDeadAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(job.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(job.getClaimToken()).isNull();
    }

    @Test
    void markDeadFromDispatchableState_shouldWorkOnPending() {
        ReminderEmailOutbox job = pendingJob();

        job.markDeadFromDispatchableState("attempts exhausted", NOW);

        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.DEAD);
    }

    @Test
    void markDeadFromDispatchableState_shouldRejectProcessingState() {
        ReminderEmailOutbox job = claimed();

        assertThatThrownBy(() -> job.markDeadFromDispatchableState("x", NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void releaseStaleClaim_shouldReturnToFailedWithNewNextAttempt() {
        ReminderEmailOutbox job = claimed();
        Instant retryAt = NOW.plusSeconds(600);

        job.releaseStaleClaim("stale worker", retryAt);

        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.FAILED);
        assertThat(job.getNextAttemptAt()).isEqualTo(retryAt);
        assertThat(job.getClaimToken()).isNull();
        assertThat(job.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void releaseStaleClaim_shouldBeNoOpForNonProcessingState() {
        ReminderEmailOutbox job = pendingJob();

        job.releaseStaleClaim("stale worker", NOW.plusSeconds(100));

        // Behavior: silently returns for PENDING; no field changes.
        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.PENDING);
        assertThat(job.getNextAttemptAt()).isEqualTo(NOW);
    }

    @Test
    void isOwnedByClaim_shouldRejectNullOrBlankExpectedToken() {
        ReminderEmailOutbox job = claimed();

        assertThat(job.isOwnedByClaim(null)).isFalse();
        assertThat(job.isOwnedByClaim("")).isFalse();
        assertThat(job.isOwnedByClaim("   ")).isFalse();
    }

    private static ReminderEmailOutbox pendingJob() {
        return ReminderEmailOutbox.pending(
                1L,
                NOW,
                FINAL_DAY,
                DEADLINE,
                99L,
                "recipient@example.test",
                "Recipient",
                NOW
        );
    }

    private static ReminderEmailOutbox claimed() {
        ReminderEmailOutbox job = pendingJob();
        job.claim(WORKER, CLAIM_TOKEN, NOW);
        return job;
    }
}
