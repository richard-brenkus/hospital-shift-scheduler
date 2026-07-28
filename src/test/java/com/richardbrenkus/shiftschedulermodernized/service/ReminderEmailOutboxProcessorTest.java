package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.exception.PermanentEmailDeliveryException;
import com.richardbrenkus.shiftschedulermodernized.exception.TransientEmailDeliveryException;
import com.richardbrenkus.shiftschedulermodernized.repository.ReminderEmailOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderEmailOutboxProcessorTest {

    private static final Instant NOW = Instant.parse("2026-08-05T08:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private ReminderEmailOutboxRepository repository;
    @Mock
    private ReminderEmailOutboxClaimService claimService;
    @Mock
    private ReminderEmailOutboxCompletionService completionService;
    @Mock
    private EmailReminderService emailReminderService;
    @Mock
    private ActivityPublisher activityPublisher;

    private ReminderEmailOutboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ReminderEmailOutboxProcessor(
                repository, claimService, completionService,
                emailReminderService, activityPublisher, FIXED_CLOCK
        );
        ReflectionTestUtils.setField(processor, "batchSize", 20);
    }

    @Test
    void processPendingReminderJobs_shouldMarkSentAndPublishSuccess_whenDispatchSucceeds() {
        when(repository.findDispatchableIds(anyList(), eq(NOW), any(Pageable.class)))
                .thenReturn(List.of(1L));
        var job = claimedJob(1L, 1);
        when(claimService.claim(eq(1L), any(String.class), eq(NOW)))
                .thenReturn(Optional.of(job));
        when(completionService.markSent(eq(1L), eq("claim-token-1"), eq(NOW)))
                .thenReturn(true);

        processor.processPendingReminderJobs();

        verify(emailReminderService).sendShiftRequestReminderEmail(
                "alice@example.test",
                "Alice",
                LocalDate.of(2026, 8, 20),
                "idem-1"
        );
        verify(completionService).markSent(eq(1L), eq("claim-token-1"), eq(NOW));
        verify(activityPublisher).publishSuccess(
                eq(ActivityType.REMINDER_EMAIL_SENT),
                eq("ReminderEmailOutbox"),
                eq("1"),
                any(String.class)
        );
    }

    @Test
    void processPendingReminderJobs_shouldScheduleRetry_onTransientFailure() {
        when(repository.findDispatchableIds(anyList(), eq(NOW), any(Pageable.class)))
                .thenReturn(List.of(1L));
        var job = claimedJob(1L, 1);
        when(claimService.claim(eq(1L), any(String.class), eq(NOW)))
                .thenReturn(Optional.of(job));
        doThrow(new TransientEmailDeliveryException("smtp temporary"))
                .when(emailReminderService).sendShiftRequestReminderEmail(
                        any(), any(), any(), any());
        when(completionService.markTransientFailure(
                eq(1L), eq("claim-token-1"), eq(NOW), any(String.class)))
                .thenReturn(ReminderEmailOutboxCompletionService.FailureCompletionResult.RETRY_SCHEDULED);

        processor.processPendingReminderJobs();

        verify(completionService).markTransientFailure(
                eq(1L), eq("claim-token-1"), eq(NOW), any(String.class));
        verify(completionService, never()).markSent(any(), any(), any());
        verify(activityPublisher).publishFailure(
                eq(ActivityType.REMINDER_EMAIL_FAILED),
                eq("ReminderEmailOutbox"),
                eq("1"),
                any(String.class),
                any(String.class),
                any()
        );
    }

    @Test
    void processPendingReminderJobs_shouldMoveToDead_onPermanentFailure() {
        when(repository.findDispatchableIds(anyList(), eq(NOW), any(Pageable.class)))
                .thenReturn(List.of(1L));
        var job = claimedJob(1L, 1);
        when(claimService.claim(eq(1L), any(String.class), eq(NOW)))
                .thenReturn(Optional.of(job));
        doThrow(new PermanentEmailDeliveryException("invalid recipient"))
                .when(emailReminderService).sendShiftRequestReminderEmail(
                        any(), any(), any(), any());
        when(completionService.markPermanentFailure(
                eq(1L), eq("claim-token-1"), eq(NOW), any(String.class)))
                .thenReturn(ReminderEmailOutboxCompletionService.FailureCompletionResult.DEAD);

        processor.processPendingReminderJobs();

        verify(completionService).markPermanentFailure(
                eq(1L), eq("claim-token-1"), eq(NOW), any(String.class));
        // DEAD → published as failure with dead-suffixed description
        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(activityPublisher).publishFailure(
                eq(ActivityType.REMINDER_EMAIL_FAILED),
                eq("ReminderEmailOutbox"),
                eq("1"),
                description.capture(),
                any(String.class),
                any()
        );
        assertThat(description.getValue()).contains("DEAD");
    }

    @Test
    void processPendingReminderJobs_shouldNotDispatch_whenClaimReturnsEmpty() {
        // Simulates the "second worker cannot re-claim" case: the row was already taken.
        when(repository.findDispatchableIds(anyList(), eq(NOW), any(Pageable.class)))
                .thenReturn(List.of(1L));
        when(claimService.claim(eq(1L), any(String.class), eq(NOW)))
                .thenReturn(Optional.empty());

        processor.processPendingReminderJobs();

        verifyNoInteractions(emailReminderService);
        verifyNoInteractions(completionService);
    }

    @Test
    void processPendingReminderJobs_shouldContinueLoop_whenOneJobFailsUnexpectedly() {
        when(repository.findDispatchableIds(anyList(), eq(NOW), any(Pageable.class)))
                .thenReturn(List.of(1L, 2L));
        when(claimService.claim(eq(1L), any(String.class), eq(NOW)))
                .thenThrow(new RuntimeException("claim exploded"));
        var okJob = claimedJob(2L, 1);
        when(claimService.claim(eq(2L), any(String.class), eq(NOW)))
                .thenReturn(Optional.of(okJob));
        when(completionService.markSent(eq(2L), eq("claim-token-2"), eq(NOW)))
                .thenReturn(true);

        processor.processPendingReminderJobs();

        // Second candidate must still be processed even if the first blew up.
        verify(emailReminderService).sendShiftRequestReminderEmail(
                any(), any(), any(), eq("idem-2"));
    }

    @Test
    void processPendingReminderJobs_shouldValidateBatchSizeBeforeQuerying() {
        ReflectionTestUtils.setField(processor, "batchSize", 0);

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> processor.processPendingReminderJobs())
                .isInstanceOf(IllegalStateException.class);
    }

    private static ReminderEmailOutboxClaimService.ClaimedReminderEmailJob claimedJob(long id, int attempt) {
        return new ReminderEmailOutboxClaimService.ClaimedReminderEmailJob(
                id,
                "claim-token-" + id,
                99L + id,
                "alice@example.test",
                "Alice",
                LocalDate.of(2026, 8, 20),
                "idem-" + id,
                attempt
        );
    }
}
