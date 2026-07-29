package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.entity.ReminderEmailOutbox;
import com.richardbrenkus.shiftschedulermodernized.repository.ReminderEmailOutboxRepository;
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
class ReminderEmailOutboxClaimServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-05T08:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-08-20T21:59:59Z");
    private static final LocalDate FINAL_DAY = LocalDate.of(2026, 8, 20);
    private static final String WORKER = "worker-1";
    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private ReminderEmailOutboxRepository repository;

    @InjectMocks
    private ReminderEmailOutboxClaimService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maximumAttempts", MAX_ATTEMPTS);
    }

    @Test
    void claim_shouldReturnClaimedJob_whenDispatchable() {
        ReminderEmailOutbox job = pendingJob();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        var claimed = service.claim(1L, WORKER, NOW);

        assertThat(claimed).isPresent();
        assertThat(claimed.get().outboxId()).isEqualTo(job.getId());
        assertThat(claimed.get().claimToken()).isNotBlank();
        assertThat(claimed.get().attemptNumber()).isEqualTo(1);
        verify(repository).saveAndFlush(job);
    }

    @Test
    void claim_shouldReturnEmpty_whenJobMissing() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThat(service.claim(1L, WORKER, NOW)).isEmpty();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void claim_shouldReturnEmpty_whenNextAttemptIsInFuture() {
        ReminderEmailOutbox job = pendingJob();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        // now is before nextAttemptAt (NOW)
        assertThat(service.claim(1L, WORKER, NOW.minusSeconds(60))).isEmpty();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void claim_shouldMoveJobToDead_whenAttemptCountAlreadyAtMaximum() {
        ReminderEmailOutbox job = pendingJob();
        ReflectionTestUtils.setField(job, "attemptCount", MAX_ATTEMPTS);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        var claimed = service.claim(1L, WORKER, NOW);

        assertThat(claimed).isEmpty();
        assertThat(job.getStatus()).isEqualTo(com.richardbrenkus.shiftschedulermodernized.config.constants.ReminderEmailOutboxStatus.DEAD);
        verify(repository).saveAndFlush(job);
    }

    @Test
    void claim_shouldPreventSecondClaimOfSameJob() {
        // Simulates the second worker's view of a row that has already been claimed.
        ReminderEmailOutbox job = pendingJob();
        job.claim(WORKER, "existing-token", NOW);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        // Job is now PROCESSING → not dispatchable → second claim returns empty.
        assertThat(service.claim(1L, "worker-2", NOW.plusSeconds(1))).isEmpty();
    }

    @Test
    void claim_shouldRejectNullOutboxId() {
        assertThatThrownBy(() -> service.claim(null, WORKER, NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void claim_shouldRejectBlankWorkerId() {
        assertThatThrownBy(() -> service.claim(1L, " ", NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void claim_shouldRejectNonPositiveMaximumAttemptsConfig() {
        ReflectionTestUtils.setField(service, "maximumAttempts", 0);
        assertThatThrownBy(() -> service.claim(1L, WORKER, NOW)).isInstanceOf(IllegalStateException.class);
    }

    private static ReminderEmailOutbox pendingJob() {
        ReminderEmailOutbox job = ReminderEmailOutbox.pending(1L, NOW, FINAL_DAY, DEADLINE, 99L, "alice@example.test", "Alice", NOW);
        ReflectionTestUtils.setField(job, "id", 1L);
        return job;
    }
}
