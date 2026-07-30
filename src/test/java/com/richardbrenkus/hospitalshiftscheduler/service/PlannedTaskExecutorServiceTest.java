package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.activity.ActivityPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/*
 * NOTE: The pre-outbox executor tests were structurally obsolete after the
 * planned-task subsystem was refactored to delegate to
 * {@link CleanupTaskExecutionService} and {@link PlannedTaskDispatchService}.
 * They have been replaced with minimal tests that exercise the current
 * delegation contract without asserting the internal execution details of the
 * two collaborators (which have their own dedicated tests).
 */
@ExtendWith(MockitoExtension.class)
class PlannedTaskExecutorServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-15T10:00:00Z");

    @Mock
    private PlannedTaskDispatchService plannedTaskDispatchService;

    @Mock
    private CleanupTaskExecutionService cleanupTaskExecutionService;

    @Mock
    private ActivityPublisher activityPublisher;

    private final Clock fixedClock = Clock.fixed(NOW, ZoneId.of("Europe/Prague"));

    private PlannedTaskExecutorService service;

    @BeforeEach
    void setUp() {
        service = new PlannedTaskExecutorService(plannedTaskDispatchService, cleanupTaskExecutionService, activityPublisher, fixedClock);
    }

    @Test
    void shouldDelegateBothCleanupAndReminderCreation_whenExecutingDueTasks() {
        service.executeDueTasks();

        verify(cleanupTaskExecutionService).executeCleanupTaskIfDue(NOW);
        verify(plannedTaskDispatchService).createReminderOutboxJobsIfDue(NOW);
    }

    @Test
    void shouldPublishCleanupFailure_whenCleanupExecutionThrows() {
        doThrow(new RuntimeException("boom")).when(cleanupTaskExecutionService).executeCleanupTaskIfDue(any(Instant.class));

        service.executeDueTasks();

        verify(activityPublisher).publishFailure(any(), any(), any(), any(), any(), any());
        verify(plannedTaskDispatchService).createReminderOutboxJobsIfDue(NOW);
    }

    @Test
    void shouldPublishReminderFailure_whenReminderCreationThrows() {
        doThrow(new RuntimeException("boom")).when(plannedTaskDispatchService).createReminderOutboxJobsIfDue(any(Instant.class));

        service.executeDueTasks();

        verify(cleanupTaskExecutionService).executeCleanupTaskIfDue(NOW);
        verify(activityPublisher).publishFailure(any(), any(), any(), any(), any(), any());
    }
}
