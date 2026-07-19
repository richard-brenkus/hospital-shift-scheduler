package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.entity.CleanupTask;
import com.richardbrenkus.shiftschedulermodernized.entity.SendReminderTask;
import com.richardbrenkus.shiftschedulermodernized.repository.CleanupTaskRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.SendReminderTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannedTaskExecutorServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 10, 0);

    @Mock
    private CleanupTaskRepository cleanupTaskRepository;

    @Mock
    private SendReminderTaskRepository sendReminderTaskRepository;

    @Mock
    private ShiftRequestService shiftRequestService;

    @Mock
    private EmailReminderService emailReminderService;

    private PlannedTaskExecutorService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                NOW.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        service = new PlannedTaskExecutorService(
                cleanupTaskRepository,
                sendReminderTaskRepository,
                shiftRequestService,
                emailReminderService,
                fixedClock
        );
    }

    @Test
    void shouldRunCleanupAndDeactivateTask_whenCleanupIsDue() {
        CleanupTask cleanup = CleanupTask.builder()
                .id(1L)
                .isActive(true)
                .executionTime(NOW.minusMinutes(5))
                .build();
        when(cleanupTaskRepository.findFirstByIsActiveTrueOrderByExecutionTimeAsc())
                .thenReturn(Optional.of(cleanup));
        when(sendReminderTaskRepository.findFirstByIsActiveTrueOrderByStartSendingTimeAsc())
                .thenReturn(Optional.empty());

        service.executeDueTasks();

        verify(shiftRequestService).deleteAllShiftRequests();
        assertThat(cleanup.isActive()).isFalse();
        verify(cleanupTaskRepository).saveAndFlush(cleanup);
    }

    @Test
    void shouldSkipCleanup_whenExecutionTimeIsInFuture() {
        CleanupTask cleanup = CleanupTask.builder()
                .id(1L)
                .isActive(true)
                .executionTime(NOW.plusHours(1))
                .build();
        when(cleanupTaskRepository.findFirstByIsActiveTrueOrderByExecutionTimeAsc())
                .thenReturn(Optional.of(cleanup));
        when(sendReminderTaskRepository.findFirstByIsActiveTrueOrderByStartSendingTimeAsc())
                .thenReturn(Optional.empty());

        service.executeDueTasks();

        verifyNoInteractions(shiftRequestService);
        verify(cleanupTaskRepository, never()).save(any());
        assertThat(cleanup.isActive()).isTrue();
    }

    @Test
    void shouldSkipCleanup_whenExecutionTimeIsNull() {
        CleanupTask cleanup = CleanupTask.builder()
                .id(1L)
                .isActive(true)
                .executionTime(null)
                .build();
        when(cleanupTaskRepository.findFirstByIsActiveTrueOrderByExecutionTimeAsc())
                .thenReturn(Optional.of(cleanup));
        when(sendReminderTaskRepository.findFirstByIsActiveTrueOrderByStartSendingTimeAsc())
                .thenReturn(Optional.empty());

        service.executeDueTasks();

        verifyNoInteractions(shiftRequestService);
        verify(cleanupTaskRepository, never()).save(any());
    }

    @Test
    void shouldRunReminderAndRescheduleNextRun_whenNotAtRepetitionCap() {
        SendReminderTask reminder = SendReminderTask.builder()
                .id(5L)
                .isActive(true)
                .startSendingTime(NOW.minusMinutes(10))
                .finalSubmissionDay(25)
                .frequencyInDays(2)
                .repetitions(3)
                .counter(0)
                .build();
        when(cleanupTaskRepository.findFirstByIsActiveTrueOrderByExecutionTimeAsc())
                .thenReturn(Optional.empty());
        when(sendReminderTaskRepository.findFirstByIsActiveTrueOrderByStartSendingTimeAsc())
                .thenReturn(Optional.of(reminder));

        service.executeDueTasks();

        verify(emailReminderService).sendShiftRequestReminderEmails(25);
        assertThat(reminder.getCounter()).isEqualTo(1);
        assertThat(reminder.isActive()).isTrue();
        assertThat(reminder.getStartSendingTime())
                .isEqualTo(NOW.minusMinutes(10).plusDays(2));
        verify(sendReminderTaskRepository).saveAndFlush(reminder);
    }

    @Test
    void shouldRunReminderAndDeactivate_whenReachingRepetitionCap() {
        SendReminderTask reminder = SendReminderTask.builder()
                .id(5L)
                .isActive(true)
                .startSendingTime(NOW.minusMinutes(10))
                .finalSubmissionDay(20)
                .frequencyInDays(2)
                .repetitions(2)
                .counter(1)
                .build();
        when(cleanupTaskRepository.findFirstByIsActiveTrueOrderByExecutionTimeAsc())
                .thenReturn(Optional.empty());
        when(sendReminderTaskRepository.findFirstByIsActiveTrueOrderByStartSendingTimeAsc())
                .thenReturn(Optional.of(reminder));

        service.executeDueTasks();

        verify(emailReminderService).sendShiftRequestReminderEmails(20);
        assertThat(reminder.getCounter()).isEqualTo(2);
        assertThat(reminder.isActive()).isFalse();
        verify(sendReminderTaskRepository).saveAndFlush(reminder);
    }

    @Test
    void shouldSkipReminder_whenStartSendingTimeIsInFuture() {
        SendReminderTask reminder = SendReminderTask.builder()
                .id(5L)
                .isActive(true)
                .startSendingTime(NOW.plusDays(2))
                .finalSubmissionDay(20)
                .frequencyInDays(2)
                .repetitions(3)
                .counter(0)
                .build();
        when(cleanupTaskRepository.findFirstByIsActiveTrueOrderByExecutionTimeAsc())
                .thenReturn(Optional.empty());
        when(sendReminderTaskRepository.findFirstByIsActiveTrueOrderByStartSendingTimeAsc())
                .thenReturn(Optional.of(reminder));

        service.executeDueTasks();

        verifyNoInteractions(emailReminderService);
        verify(sendReminderTaskRepository, never()).save(any());
    }

    @Test
    void shouldDoNothing_whenNeitherTaskIsPresent() {
        when(cleanupTaskRepository.findFirstByIsActiveTrueOrderByExecutionTimeAsc())
                .thenReturn(Optional.empty());
        when(sendReminderTaskRepository.findFirstByIsActiveTrueOrderByStartSendingTimeAsc())
                .thenReturn(Optional.empty());

        service.executeDueTasks();

        verifyNoInteractions(shiftRequestService);
        verifyNoInteractions(emailReminderService);
        verify(cleanupTaskRepository, never()).save(any());
        verify(sendReminderTaskRepository, never()).save(any());
    }

    @Test
    void clockShouldControlNowUsedByExecutor() {
        // Confirms that the Clock instance drives "now" — if the fixed clock is later than the reminder start,
        // the reminder is due and runs; if earlier, it does not. This test uses a different fixed clock explicitly.
        Clock earlyClock = Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneId.of("UTC")
        );
        PlannedTaskExecutorService earlyService = new PlannedTaskExecutorService(
                cleanupTaskRepository,
                sendReminderTaskRepository,
                shiftRequestService,
                emailReminderService,
                earlyClock
        );

        SendReminderTask reminder = SendReminderTask.builder()
                .id(5L)
                .isActive(true)
                .startSendingTime(LocalDate.of(2026, 6, 1).atStartOfDay())
                .repetitions(3)
                .counter(0)
                .frequencyInDays(1)
                .finalSubmissionDay(20)
                .build();
        when(cleanupTaskRepository.findFirstByIsActiveTrueOrderByExecutionTimeAsc())
                .thenReturn(Optional.empty());
        when(sendReminderTaskRepository.findFirstByIsActiveTrueOrderByStartSendingTimeAsc())
                .thenReturn(Optional.of(reminder));

        earlyService.executeDueTasks();

        verifyNoInteractions(emailReminderService);
    }
}
