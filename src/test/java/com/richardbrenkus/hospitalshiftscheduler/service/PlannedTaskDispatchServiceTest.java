package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.activity.ActivityPublisher;
import com.richardbrenkus.hospitalshiftscheduler.entity.ReminderEmailOutbox;
import com.richardbrenkus.hospitalshiftscheduler.entity.SendReminderTask;
import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import com.richardbrenkus.hospitalshiftscheduler.repository.ReminderEmailOutboxRepository;
import com.richardbrenkus.hospitalshiftscheduler.repository.SendReminderTaskRepository;
import com.richardbrenkus.hospitalshiftscheduler.repository.UserRepository;
import com.richardbrenkus.hospitalshiftscheduler.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannedTaskDispatchServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Prague");
    private static final Instant NOW = Instant.parse("2026-08-05T08:00:00Z");

    @Mock
    private SendReminderTaskRepository sendReminderTaskRepository;

    @Mock
    private ReminderEmailOutboxRepository reminderEmailOutboxRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityPublisher activityPublisher;

    private PlannedTaskDispatchService service;

    @BeforeEach
    void setUp() {
        service = new PlannedTaskDispatchService(sendReminderTaskRepository, reminderEmailOutboxRepository, userRepository, activityPublisher, ZONE);
    }

    @Test
    void createReminderOutboxJobsIfDue_shouldRejectNullNow() {
        assertThatThrownBy(() -> service.createReminderOutboxJobsIfDue(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createReminderOutboxJobsIfDue_shouldCreateExactlyOneJobPerEligibleUser() {
        SendReminderTask task = dueTask();
        when(sendReminderTaskRepository.findByIdForUpdate(SendReminderTask.SINGLETON_ID)).thenReturn(Optional.of(task));

        User recipient = TestFixtures.user(42L, "alice");
        recipient.setEmail("alice@example.test");
        recipient.setName("Alice");
        when(userRepository.findByShiftRequestIsNullOrderByNameAsc()).thenReturn(List.of(recipient));
        when(reminderEmailOutboxRepository.existsBySourceTaskIdAndScheduledExecutionTimeAndRecipientUserId(eq(task.getId()), eq(task.getStartSendingTime()), eq(42L))).thenReturn(false);

        service.createReminderOutboxJobsIfDue(NOW);

        ArgumentCaptor<ReminderEmailOutbox> saved = ArgumentCaptor.forClass(ReminderEmailOutbox.class);
        verify(reminderEmailOutboxRepository).save(saved.capture());
        assertThat(saved.getValue().getRecipientUserId()).isEqualTo(42L);
        assertThat(saved.getValue().getRecipientEmail()).isEqualTo("alice@example.test");
    }

    @Test
    void createReminderOutboxJobsIfDue_shouldNotDuplicateExistingJob() {
        SendReminderTask task = dueTask();
        when(sendReminderTaskRepository.findByIdForUpdate(SendReminderTask.SINGLETON_ID)).thenReturn(Optional.of(task));

        User recipient = TestFixtures.user(42L, "alice");
        recipient.setEmail("alice@example.test");
        when(userRepository.findByShiftRequestIsNullOrderByNameAsc()).thenReturn(List.of(recipient));
        // The dedup guard says "already queued".
        when(reminderEmailOutboxRepository.existsBySourceTaskIdAndScheduledExecutionTimeAndRecipientUserId(eq(task.getId()), eq(task.getStartSendingTime()), eq(42L))).thenReturn(true);

        service.createReminderOutboxJobsIfDue(NOW);

        verify(reminderEmailOutboxRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createReminderOutboxJobsIfDue_shouldSkipIneligibleRecipients() {
        SendReminderTask task = dueTask();
        when(sendReminderTaskRepository.findByIdForUpdate(SendReminderTask.SINGLETON_ID)).thenReturn(Optional.of(task));

        User noEmail = TestFixtures.user(50L, "no-email");
        noEmail.setEmail(null);
        User blankEmail = TestFixtures.user(51L, "blank-email");
        blankEmail.setEmail("   ");
        when(userRepository.findByShiftRequestIsNullOrderByNameAsc()).thenReturn(List.of(noEmail, blankEmail));

        service.createReminderOutboxJobsIfDue(NOW);

        verify(reminderEmailOutboxRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createReminderOutboxJobsIfDue_shouldReturnEarlyWhenTaskInactive() {
        SendReminderTask task = new SendReminderTask();
        task.setId(SendReminderTask.SINGLETON_ID);
        task.setActive(false);

        when(sendReminderTaskRepository.findByIdForUpdate(SendReminderTask.SINGLETON_ID)).thenReturn(Optional.of(task));

        service.createReminderOutboxJobsIfDue(NOW);

        verifyNoInteractions(userRepository);
        verify(reminderEmailOutboxRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(activityPublisher);
    }

    @Test
    void createReminderOutboxJobsIfDue_shouldReturnEarlyWhenStartTimeIsInFuture() {
        SendReminderTask task = dueTask();
        task.setStartSendingTime(NOW.plusSeconds(3600));
        when(sendReminderTaskRepository.findByIdForUpdate(SendReminderTask.SINGLETON_ID)).thenReturn(Optional.of(task));

        service.createReminderOutboxJobsIfDue(NOW);

        verifyNoInteractions(userRepository);
        verify(reminderEmailOutboxRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createReminderOutboxJobsIfDue_shouldThrowIllegalStateWhenSingletonMissing() {
        when(sendReminderTaskRepository.findByIdForUpdate(SendReminderTask.SINGLETON_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createReminderOutboxJobsIfDue(NOW)).isInstanceOf(IllegalStateException.class);
    }

    private static SendReminderTask dueTask() {
        SendReminderTask sendReminderTask = new SendReminderTask();
        sendReminderTask.setId(SendReminderTask.SINGLETON_ID);
        sendReminderTask.setActive(true);
        sendReminderTask.setStartSendingTime(NOW);
        sendReminderTask.setFinalRequestSubmissionDate(LocalDate.of(2026, 8, 20));
        sendReminderTask.setRepetitions(3);
        sendReminderTask.setFrequencyInDays(2);
        sendReminderTask.setCounter(0);

        return sendReminderTask;
    }
}
