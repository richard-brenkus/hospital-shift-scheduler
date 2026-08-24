package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.ReminderEmailOutboxStatus;
import com.richardbrenkus.hospitalshiftscheduler.entity.ReminderEmailOutbox;
import com.richardbrenkus.hospitalshiftscheduler.repository.ReminderEmailOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderEmailOutboxCompletionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-05T08:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-08-20T21:59:59Z");
    private static final LocalDate FINAL_DAY = LocalDate.of(2026, 8, 20);
    private static final String WORKER = "worker-1";
    private static final String CLAIM_TOKEN = "claim-1";
    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private ReminderEmailOutboxRepository repository;

    @InjectMocks
    private ReminderEmailOutboxCompletionService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maximumAttempts", MAX_ATTEMPTS);
    }

    @Test
    void markSent_shouldTransitionJobAndReturnTrue() {
        ReminderEmailOutbox job = claimedJob();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        boolean changed = service.markSent(1L, CLAIM_TOKEN, NOW.plusSeconds(1));

        assertThat(changed).isTrue();
        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.SENT);
        verify(repository).saveAndFlush(job);
    }

    @Test
    void markSent_shouldReturnFalse_whenClaimTokenNoLongerOwnsRow() {
        ReminderEmailOutbox job = claimedJob();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        boolean changed = service.markSent(1L, "wrong-token", NOW.plusSeconds(1));

        assertThat(changed).isFalse();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void markSent_shouldReturnFalse_whenJobMissing() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThat(service.markSent(1L, CLAIM_TOKEN, NOW.plusSeconds(1))).isFalse();
    }

    @Test
    void markTransientFailure_shouldMoveJobBackToFailedAndScheduleRetry() {
        ReminderEmailOutbox job = claimedJob();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        ReminderEmailOutboxCompletionService.FailureCompletionResult result = service.markTransientFailure(1L, CLAIM_TOKEN, NOW.plusSeconds(1), "smtp error");

        assertThat(result).isEqualTo(ReminderEmailOutboxCompletionService.FailureCompletionResult.RETRY_SCHEDULED);
        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.FAILED);
        assertThat(job.getLastFailureReason()).isEqualTo("smtp error");
        // First retry: 1 minute after now.
        assertThat(job.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(1).plusSeconds(60));
        verify(repository).saveAndFlush(job);
    }

    @Test
    void markTransientFailure_shouldReturnDead_whenAttemptCountReachesMaximum() {
        // Claim increments attemptCount to 1. Force it to MAX_ATTEMPTS to simulate
        // the third failed attempt in a row.
        ReminderEmailOutbox job = claimedJob();
        ReflectionTestUtils.setField(job, "attemptCount", MAX_ATTEMPTS);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        ReminderEmailOutboxCompletionService.FailureCompletionResult result = service.markTransientFailure(1L, CLAIM_TOKEN, NOW.plusSeconds(1), "smtp error");

        assertThat(result).isEqualTo(ReminderEmailOutboxCompletionService.FailureCompletionResult.DEAD);
        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.DEAD);
    }

    @Test
    void markTransientFailure_shouldReturnNotChanged_whenClaimTokenMismatch() {
        ReminderEmailOutbox job = claimedJob();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        ReminderEmailOutboxCompletionService.FailureCompletionResult result = service.markTransientFailure(1L, "wrong-token", NOW.plusSeconds(1), "smtp error");

        assertThat(result).isEqualTo(ReminderEmailOutboxCompletionService.FailureCompletionResult.NOT_CHANGED);
        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.PROCESSING);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void markPermanentFailure_shouldMoveJobToDead() {
        ReminderEmailOutbox job = claimedJob();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        ReminderEmailOutboxCompletionService.FailureCompletionResult result = service.markPermanentFailure(1L, CLAIM_TOKEN, NOW.plusSeconds(1), "invalid recipient");

        assertThat(result).isEqualTo(ReminderEmailOutboxCompletionService.FailureCompletionResult.DEAD);
        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.DEAD);
        assertThat(job.getLastFailureReason()).isEqualTo("invalid recipient");
    }

    @Test
    void markPermanentFailure_shouldReturnNotChanged_whenClaimTokenMismatch() {
        ReminderEmailOutbox job = claimedJob();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        ReminderEmailOutboxCompletionService.FailureCompletionResult result = service.markPermanentFailure(1L, "wrong-token", NOW.plusSeconds(1), "invalid recipient");

        assertThat(result).isEqualTo(ReminderEmailOutboxCompletionService.FailureCompletionResult.NOT_CHANGED);
    }

    @Test
    void markSent_shouldRejectNullArguments() {
        assertThatThrownBy(() -> service.markSent(null, CLAIM_TOKEN, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.markSent(1L, " ", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.markSent(1L, CLAIM_TOKEN, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelClaimedJob_shouldDeleteRowAndReturnTrue_whenClaimTokenMatches() {
        ReminderEmailOutbox job = claimedJob();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        boolean canceled = service.cancelClaimedJob(1L, CLAIM_TOKEN);

        assertThat(canceled).isTrue();
        verify(repository).delete(job);
        verify(repository).flush();
    }

    @Test
    void cancelClaimedJob_shouldReturnFalseAndNotDelete_whenClaimTokenMismatch() {
        ReminderEmailOutbox job = claimedJob();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        boolean canceled = service.cancelClaimedJob(1L, "wrong-token");

        assertThat(canceled).isFalse();
        verify(repository, never()).delete(any());
    }

    private static ReminderEmailOutbox claimedJob() {
        ReminderEmailOutbox job = ReminderEmailOutbox.pending(1L, NOW, FINAL_DAY, DEADLINE, 99L, "alice@example.test", "Alice", NOW);
        ReflectionTestUtils.setField(job, "id", 1L);
        job.claim(WORKER, CLAIM_TOKEN, NOW);
        return job;
    }
}
