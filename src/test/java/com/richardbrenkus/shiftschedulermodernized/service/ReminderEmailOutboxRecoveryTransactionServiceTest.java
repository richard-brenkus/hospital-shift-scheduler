package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ReminderEmailOutboxStatus;
import com.richardbrenkus.shiftschedulermodernized.entity.ReminderEmailOutbox;
import com.richardbrenkus.shiftschedulermodernized.repository.ReminderEmailOutboxRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderEmailOutboxRecoveryTransactionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-05T08:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-08-20T21:59:59Z");
    private static final LocalDate FINAL_DAY = LocalDate.of(2026, 8, 20);

    @Mock
    private ReminderEmailOutboxRepository repository;

    @InjectMocks
    private ReminderEmailOutboxRecoveryTransactionService service;

    @Test
    void releaseStaleClaim_shouldReturnFalse_whenJobIsNotProcessing() {
        // The job is PENDING → not stale, nothing to release.
        ReminderEmailOutbox job = pendingJob();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        boolean released = service.releaseStaleClaim(
                1L,
                NOW.minusSeconds(600),
                NOW.plusSeconds(60)
        );

        assertThat(released).isFalse();
        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.PENDING);
    }

    @Test
    void releaseStaleClaim_shouldReturnFalse_whenJobMissing() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThat(service.releaseStaleClaim(
                1L, NOW.minusSeconds(600), NOW.plusSeconds(60)
        )).isFalse();
    }

    @Test
    void releaseStaleClaim_shouldReturnFalse_whenClaimedAfterStaleThreshold() {
        // Job is PROCESSING but was claimed AFTER the stale-before cutoff.
        ReminderEmailOutbox job = pendingJob();
        Instant claimedAt = NOW.minusSeconds(30);
        job.claim("worker", "token", claimedAt);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        boolean released = service.releaseStaleClaim(
                1L,
                NOW.minusSeconds(300), // stale-before is older than claimedAt
                NOW.plusSeconds(60)
        );

        assertThat(released).isFalse();
        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.PROCESSING);
    }

    @Test
    void releaseStaleClaim_shouldMoveToFailed_whenProcessingClaimIsStale() {
        ReminderEmailOutbox job = pendingJob();
        Instant claimedAt = NOW.minusSeconds(600);
        job.claim("worker", "token", claimedAt);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(job));

        boolean released = service.releaseStaleClaim(
                1L,
                NOW.minusSeconds(300), // stale-before newer than claimedAt
                NOW.plusSeconds(60)
        );

        assertThat(released).isTrue();
        assertThat(job.getStatus()).isEqualTo(ReminderEmailOutboxStatus.FAILED);
        assertThat(job.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(60));
    }

    private static ReminderEmailOutbox pendingJob() {
        ReminderEmailOutbox job = ReminderEmailOutbox.pending(
                1L, NOW.minusSeconds(1000), FINAL_DAY, DEADLINE,
                99L, "alice@example.test", "Alice", NOW.minusSeconds(1000)
        );
        ReflectionTestUtils.setField(job, "id", 1L);
        return job;
    }
}
