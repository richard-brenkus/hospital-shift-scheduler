package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CleanupTaskForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.SendReminderTaskForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.CleanupTaskRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.SendReminderTaskRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.CleanupTask;
import com.richardbrenkus.shiftschedulermodernized.entity.SendReminderTask;
import com.richardbrenkus.shiftschedulermodernized.repository.CleanupTaskRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.SendReminderTaskRepository;
import com.richardbrenkus.shiftschedulermodernized.util.CalendarDateIdUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannedTasksServiceTest {

    @Mock
    private CleanupTaskRepository cleanupTaskRepository;

    @Mock
    private SendReminderTaskRepository sendReminderTaskRepository;

    @Mock
    private ActivityPublisher activityPublisher;

    @InjectMocks
    private PlannedTasksService service;

    @Test
    void shouldOnlyDeactivateExistingTasks_whenCleanupTaskIsMarkedInactive() {
        CleanupTaskForm form = new CleanupTaskForm();
        form.setCleanupTaskActive(false);
        CleanupTask activeExisting = CleanupTask.builder()
                .id(1L)
                .isActive(true)
                .executionTime(LocalDateTime.now())
                .build();
        CleanupTask inactiveExisting = CleanupTask.builder().id(2L).isActive(false).build();
        when(cleanupTaskRepository.findAll()).thenReturn(List.of(activeExisting, inactiveExisting));

        service.saveCleanupTask(form);

        assertThat(activeExisting.isActive()).isFalse();
        verify(cleanupTaskRepository).saveAndFlush(activeExisting);
        verify(cleanupTaskRepository, never()).save(inactiveExisting);
    }

    @Test
    void shouldSaveNewActiveCleanupTask_whenFormIsActiveAndNoTaskExists() {
        CleanupTaskForm form = CleanupTaskForm.builder()
                .isCleanupTaskActive(true)
                .cleanupDay(10)
                .cleanupHour(6)
                .cleanupMinute(45)
                .build();
        when(cleanupTaskRepository.findAll()).thenReturn(List.of());

        service.saveCleanupTask(form);

        ArgumentCaptor<CleanupTask> captor = ArgumentCaptor.forClass(CleanupTask.class);
        verify(cleanupTaskRepository).saveAndFlush(captor.capture());
        CleanupTask saved = captor.getValue();
        assertThat(saved.isActive()).isTrue();
        LocalDateTime expected = expectedNextMonthDateTime(10, 6, 45);
        assertThat(saved.getExecutionTime()).isEqualTo(expected);
        assertThat(saved.getCreationTime()).isNotNull();
    }

    @Test
    void shouldClampCleanupToEndOfNextMonth_whenDayIsGreaterThanMaxLength() {
        CleanupTaskForm form = CleanupTaskForm.builder()
                .isCleanupTaskActive(true)
                .cleanupDay(50)
                .cleanupHour(1)
                .cleanupMinute(1)
                .build();
        when(cleanupTaskRepository.findAll()).thenReturn(List.of());

        service.saveCleanupTask(form);

        ArgumentCaptor<CleanupTask> captor = ArgumentCaptor.forClass(CleanupTask.class);
        verify(cleanupTaskRepository).saveAndFlush(captor.capture());

        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        LocalDateTime expected = LocalDateTime.of(nextMonth.atEndOfMonth(), java.time.LocalTime.MAX);
        assertThat(captor.getValue().getExecutionTime()).isEqualTo(expected);
    }

    @Test
    void shouldDeactivateThenReactivateExistingTask_whenSavingActiveCleanupWithExisting() {
        CleanupTask existing = CleanupTask.builder()
                .id(1L)
                .isActive(true)
                .creationTime(LocalDateTime.now().minusDays(5))
                .build();
        CleanupTaskForm form = CleanupTaskForm.builder()
                .isCleanupTaskActive(true)
                .cleanupDay(5)
                .cleanupHour(9)
                .cleanupMinute(30)
                .build();
        when(cleanupTaskRepository.findAll()).thenReturn(List.of(existing));

        service.saveCleanupTask(form);

        // deactivateAllCleanupTasks saves it once, and then final save reactivates+persists
        verify(cleanupTaskRepository, times(2)).saveAndFlush(existing);
        assertThat(existing.isActive()).isTrue();
    }

    @Test
    void shouldOnlyDeactivateSendReminders_whenFormIsMarkedInactive() {
        SendReminderTaskForm form = new SendReminderTaskForm();
        form.setSendReminderTaskActive(false);
        SendReminderTask activeExisting = SendReminderTask.builder()
                .id(1L)
                .isActive(true)
                .build();
        when(sendReminderTaskRepository.findAll()).thenReturn(List.of(activeExisting));

        service.saveSendReminderTask(form);

        assertThat(activeExisting.isActive()).isFalse();
        verify(sendReminderTaskRepository).saveAndFlush(activeExisting);
    }

    @Test
    void shouldSaveNewActiveReminderTask_whenNoneExistYet() {
        SendReminderTaskForm form = SendReminderTaskForm.builder()
                .isSendReminderTaskActive(true)
                .startSendingRemindersDay(5)
                .startSendingRemindersHour(9)
                .startSendingRemindersMinute(30)
                .reminderRepetitions(3)
                .reminderSendingFrequencyInDays(2)
                .finalSubmissionDay(20)
                .build();
        when(sendReminderTaskRepository.findAll()).thenReturn(List.of());

        service.saveSendReminderTask(form);

        ArgumentCaptor<SendReminderTask> captor = ArgumentCaptor.forClass(SendReminderTask.class);
        verify(sendReminderTaskRepository).saveAndFlush(captor.capture());
        SendReminderTask saved = captor.getValue();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getRepetitions()).isEqualTo(3);
        assertThat(saved.getFrequencyInDays()).isEqualTo(2);
        assertThat(saved.getFinalRequestSubmissionDate().getDayOfMonth()).isEqualTo(20);
        assertThat(saved.getStartSendingTime()).isEqualTo(expectedNextMonthDateTime(5, 9, 30));
        assertThat(saved.getCreationTime()).isNotNull();
    }

    @Test
    void shouldReturnDayError_whenReminderDayIsGreaterThanOrEqualToFinalSubmissionDay() {
        SendReminderTaskForm form = SendReminderTaskForm.builder()
                .isSendReminderTaskActive(true)
                .startSendingRemindersDay(21)
                .finalSubmissionDay(20)
                .build();

        assertThat(service.hasDayError(form)).isTrue();
    }

    @Test
    void shouldNotReturnDayError_whenReminderDayIsBeforeFinalSubmissionDay() {
        SendReminderTaskForm form = SendReminderTaskForm.builder()
                .isSendReminderTaskActive(true)
                .startSendingRemindersDay(15)
                .finalSubmissionDay(20)
                .build();

        assertThat(service.hasDayError(form)).isFalse();
    }

    @Test
    void shouldNotReturnDayError_whenReminderIsInactive() {
        SendReminderTaskForm form = SendReminderTaskForm.builder()
                .isSendReminderTaskActive(false)
                .startSendingRemindersDay(25)
                .finalSubmissionDay(20)
                .build();

        assertThat(service.hasDayError(form)).isFalse();
    }

    @Test
    void shouldReturnEmptyCleanupTaskForm_whenNoActiveTaskExists() {
        when(cleanupTaskRepository.findAll()).thenReturn(List.of());

        CleanupTaskForm form = service.getCleanupTaskForm();

        assertThat(form.isCleanupTaskActive()).isFalse();
    }

    @Test
    void shouldPopulateCleanupTaskForm_whenActiveTaskExists() {
        CleanupTask task = CleanupTask.builder()
                .id(1L)
                .isActive(true)
                .executionTime(LocalDateTime.of(2026, 8, 15, 6, 45))
                .build();
        when(cleanupTaskRepository.findAll()).thenReturn(List.of(task));

        CleanupTaskForm form = service.getCleanupTaskForm();

        assertThat(form.isCleanupTaskActive()).isTrue();
        assertThat(form.getCleanupDay()).isEqualTo(15);
        assertThat(form.getCleanupHour()).isEqualTo(6);
        assertThat(form.getCleanupMinute()).isEqualTo(45);
    }

    @Test
    void shouldReturnEmptyReminderTaskForm_whenNoActiveTaskExists() {
        when(sendReminderTaskRepository.findAll()).thenReturn(List.of());

        SendReminderTaskForm form = service.getSendReminderTaskForm();

        assertThat(form.isSendReminderTaskActive()).isFalse();
    }

    @Test
    void shouldPopulateReminderTaskForm_whenActiveTaskExists() {
        SendReminderTask task = SendReminderTask.builder()
                .id(1L)
                .isActive(true)
                .startSendingTime(LocalDateTime.of(2026, 8, 5, 9, 30))
                .frequencyInDays(2)
                .repetitions(3)
                .finalRequestSubmissionDate(CalendarDateIdUtils.returnAdjustedFinalSubmissionDateTime(20))
                .build();
        when(sendReminderTaskRepository.findAll()).thenReturn(List.of(task));

        SendReminderTaskForm form = service.getSendReminderTaskForm();

        assertThat(form.isSendReminderTaskActive()).isTrue();
        assertThat(form.getStartSendingRemindersDay()).isEqualTo(5);
        assertThat(form.getStartSendingRemindersHour()).isEqualTo(9);
        assertThat(form.getStartSendingRemindersMinute()).isEqualTo(30);
        assertThat(form.getReminderSendingFrequencyInDays()).isEqualTo(2);
        assertThat(form.getReminderRepetitions()).isEqualTo(3);
        assertThat(form.getFinalSubmissionDay()).isEqualTo(20);
    }

    @Test
    void shouldReturnInactiveCleanupTaskRecord_whenNoActiveTaskExists() {
        when(cleanupTaskRepository.findAll()).thenReturn(List.of());

        CleanupTaskRecord record = service.getCleanupTaskRecord();

        assertThat(record.cleanupIsActive()).isFalse();
        assertThat(record.cleanupDateTime()).isNull();
    }

    @Test
    void shouldReturnActiveCleanupTaskRecord_whenActiveTaskExists() {
        LocalDateTime execTime = LocalDateTime.of(2026, 8, 20, 4, 0);
        CleanupTask task = CleanupTask.builder()
                .id(1L)
                .isActive(true)
                .executionTime(execTime)
                .build();
        when(cleanupTaskRepository.findAll()).thenReturn(List.of(task));

        CleanupTaskRecord record = service.getCleanupTaskRecord();

        assertThat(record.cleanupIsActive()).isTrue();
        assertThat(record.cleanupDateTime()).isEqualTo(execTime);
    }

    @Test
    void shouldReturnInactiveReminderRecord_whenNoActiveTaskExists() {
        when(sendReminderTaskRepository.findAll()).thenReturn(List.of());

        SendReminderTaskRecord record = service.getSendReminderTaskRecord();

        assertThat(record.reminderIsActive()).isFalse();
    }

    @Test
    void shouldReturnActiveReminderRecord_whenActiveTaskExists() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 5, 9, 30);
        SendReminderTask task = SendReminderTask.builder()
                .id(1L)
                .isActive(true)
                .startSendingTime(start)
                .frequencyInDays(2)
                .repetitions(3)
                .finalRequestSubmissionDate(CalendarDateIdUtils.returnAdjustedFinalSubmissionDateTime(20))
                .build();
        when(sendReminderTaskRepository.findAll()).thenReturn(List.of(task));

        SendReminderTaskRecord record = service.getSendReminderTaskRecord();

        assertThat(record.reminderIsActive()).isTrue();
        assertThat(record.reminderRepetitions()).isEqualTo(3);
        assertThat(record.reminderFrequency()).isEqualTo(2);
        assertThat(record.reminderStart()).isEqualTo(start);
        assertThat(record.reminderFinalSubmissionDay()).isEqualTo(20);
    }

    private static LocalDateTime expectedNextMonthDateTime(int day, int hour, int minute) {
        LocalDateTime nextMonth = LocalDateTime.now().plusMonths(1);
        return LocalDateTime.of(nextMonth.getYear(), nextMonth.getMonth(), day, hour, minute);
    }
}
