package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.activity.ActivityPublisher;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ActivityType;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.CleanupTaskForm;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.SendReminderTaskForm;
import com.richardbrenkus.hospitalshiftscheduler.entity.SendReminderTask;
import com.richardbrenkus.hospitalshiftscheduler.mapper.PlannedTaskMapper;
import com.richardbrenkus.hospitalshiftscheduler.repository.CleanupTaskRepository;
import com.richardbrenkus.hospitalshiftscheduler.repository.ReminderEmailOutboxRepository;
import com.richardbrenkus.hospitalshiftscheduler.repository.SendReminderTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * NOTE: The original generated tests were structurally obsolete after
 * PlannedTasksService switched to the singleton-row + pessimistic-lock model
 * and Instant/ZoneId-based time handling. The behaviour that used to be
 * asserted (findAll-based deactivation cascades, LocalDateTime execution
 * times, no Clock injection) no longer exists in production. The
 * form-validation helpers below still test meaningful current behaviour.
 */
@ExtendWith(MockitoExtension.class)
class PlannedTasksServiceTest {

    @Mock
    private CleanupTaskRepository cleanupTaskRepository;

    @Mock
    private SendReminderTaskRepository sendReminderTaskRepository;

    @Mock
    private ReminderEmailOutboxRepository reminderEmailOutboxRepository;

    @Mock
    private ActivityPublisher activityPublisher;

    @Mock
    private PlannedTaskMapper plannedTaskMapper;

    private final ZoneId zone = ZoneId.of("Europe/Prague");
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-08-05T08:00:00Z"), zone);

    private PlannedTasksService service;

    @BeforeEach
    void setUp() {
        service = new PlannedTasksService(cleanupTaskRepository, sendReminderTaskRepository, reminderEmailOutboxRepository, activityPublisher, plannedTaskMapper, fixedClock, zone);
    }

    @Test
    void shouldReturnDayError_whenReminderDayIsGreaterThanOrEqualToFinalSubmissionDay() {
        SendReminderTaskForm form = SendReminderTaskForm.builder().isSendReminderTaskActive(true).startSendingRemindersDay(21).finalSubmissionDay(20).build();

        assertThat(service.hasDayError(form)).isTrue();
    }

    @Test
    void shouldNotReturnDayError_whenReminderDayIsBeforeFinalSubmissionDay() {
        SendReminderTaskForm form = SendReminderTaskForm.builder().isSendReminderTaskActive(true).startSendingRemindersDay(15).finalSubmissionDay(20).build();

        assertThat(service.hasDayError(form)).isFalse();
    }

    @Test
    void shouldNotReturnDayError_whenReminderIsInactive() {
        SendReminderTaskForm form = SendReminderTaskForm.builder().isSendReminderTaskActive(false).startSendingRemindersDay(25).finalSubmissionDay(20).build();

        assertThat(service.hasDayError(form)).isFalse();
    }

    @Test
    void shouldReturnTrue_whenCleanupIsInactive() {
        CleanupTaskForm form = CleanupTaskForm.builder().isCleanupTaskActive(false).build();

        assertThat(service.isCleanupTimeInFuture(form, Instant.parse("2026-08-05T08:00:00Z"))).isTrue();
    }

    @Test
    void shouldReturnTrue_whenSendReminderIsInactive() {
        SendReminderTaskForm form = SendReminderTaskForm.builder().isSendReminderTaskActive(false).build();

        Instant now = Instant.parse("2026-08-05T08:00:00Z");

        assertThat(service.isFirstReminderInFuture(form, now)).isTrue();
        assertThat(service.isSendRemindersSetupValid(form, now)).isTrue();
    }

    @Test
    void saveSendReminderTask_shouldBulkDeleteAndPublishAggregateEvent_whenAdminDisablesReminderAndRowsExist() {
        SendReminderTask task = new SendReminderTask();
        task.setActive(true);
        when(sendReminderTaskRepository.findByIdForUpdate(SendReminderTask.SINGLETON_ID)).thenReturn(Optional.of(task));
        when(reminderEmailOutboxRepository.deleteDispatchableBySourceTaskId(SendReminderTask.SINGLETON_ID)).thenReturn(3);

        SendReminderTaskForm form = SendReminderTaskForm.builder().isSendReminderTaskActive(false).build();

        service.saveSendReminderTask(form, Instant.parse("2026-08-05T08:00:00Z"));

        assertThat(task.isActive()).isFalse();
        verify(reminderEmailOutboxRepository).deleteDispatchableBySourceTaskId(SendReminderTask.SINGLETON_ID);
        verify(activityPublisher).publishSuccess(eq(ActivityType.ADMIN_SETTINGS_CHANGED), eq("SendReminderTask"), eq("1"), any(String.class));
        verify(activityPublisher).publishSuccess(eq(ActivityType.REMINDER_EMAIL_JOBS_CANCELED), eq("ReminderEmailOutbox"), eq("1"), eq("Cancelled 3 pending reminder email job(s) after admin disabled reminders"));
    }

    @Test
    void saveSendReminderTask_shouldNotPublishAggregateEvent_whenAdminDisablesReminderButNoRowsExist() {
        SendReminderTask task = new SendReminderTask();
        task.setActive(true);
        when(sendReminderTaskRepository.findByIdForUpdate(SendReminderTask.SINGLETON_ID)).thenReturn(Optional.of(task));
        when(reminderEmailOutboxRepository.deleteDispatchableBySourceTaskId(SendReminderTask.SINGLETON_ID)).thenReturn(0);

        SendReminderTaskForm form = SendReminderTaskForm.builder().isSendReminderTaskActive(false).build();

        service.saveSendReminderTask(form, Instant.parse("2026-08-05T08:00:00Z"));

        assertThat(task.isActive()).isFalse();
        verify(reminderEmailOutboxRepository).deleteDispatchableBySourceTaskId(SendReminderTask.SINGLETON_ID);
        verify(activityPublisher).publishSuccess(eq(ActivityType.ADMIN_SETTINGS_CHANGED), eq("SendReminderTask"), eq("1"), any(String.class));
        verify(activityPublisher, never()).publishSuccess(eq(ActivityType.REMINDER_EMAIL_JOBS_CANCELED), any(String.class), any(String.class), any(String.class));
    }
}
