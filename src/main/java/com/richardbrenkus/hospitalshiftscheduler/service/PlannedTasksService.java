package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.activity.ActivityPublisher;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ActivityType;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.CleanupTaskForm;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.SendReminderTaskForm;
import com.richardbrenkus.hospitalshiftscheduler.dto.view.CleanupTaskRecord;
import com.richardbrenkus.hospitalshiftscheduler.dto.view.SendReminderTaskRecord;
import com.richardbrenkus.hospitalshiftscheduler.entity.CleanupTask;
import com.richardbrenkus.hospitalshiftscheduler.entity.SendReminderTask;
import com.richardbrenkus.hospitalshiftscheduler.mapper.PlannedTaskMapper;
import com.richardbrenkus.hospitalshiftscheduler.repository.CleanupTaskRepository;
import com.richardbrenkus.hospitalshiftscheduler.repository.ReminderEmailOutboxRepository;
import com.richardbrenkus.hospitalshiftscheduler.repository.SendReminderTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PlannedTasksService {

    private final CleanupTaskRepository cleanupTaskRepository;
    private final SendReminderTaskRepository sendReminderTaskRepository;
    private final ReminderEmailOutboxRepository reminderEmailOutboxRepository;
    private final ActivityPublisher activityPublisher;
    private final PlannedTaskMapper plannedTaskMapper;
    private final Clock applicationClock;
    private final ZoneId applicationZoneId;

    @Transactional
    public void saveCleanupTask(CleanupTaskForm form, Instant now) {
        Objects.requireNonNull(form, "form must not be null");

        CleanupTask task = getCleanupTaskForUpdate();

        if (!form.isCleanupTaskActive()) {
            if (task.isActive()) {
                task.setActive(false);

                activityPublisher.publishSuccess(ActivityType.ADMIN_SETTINGS_CHANGED, "CleanupTask", String.valueOf(task.getId()), "Cleanup task disabled");
            }
            return;
        }

        Instant executionTime = createCleanupInstant(form);

        if (!executionTime.isAfter(now)) {
            throw new IllegalArgumentException("Cleanup time must be in the future");
        }

        task.setActive(true);
        task.setExecutionTime(executionTime);

        if (task.getCreationTime() == null) {
            task.setCreationTime(now);
        }

        activityPublisher.publishSuccess(ActivityType.ADMIN_SETTINGS_CHANGED, "CleanupTask", String.valueOf(task.getId()), "Cleanup task configuration updated");
    }

    @Transactional
    public void saveSendReminderTask(SendReminderTaskForm form, Instant now) {
        Objects.requireNonNull(form, "form must not be null");
        Objects.requireNonNull(now, "now must not be null");

        SendReminderTask task = getSendReminderTaskForUpdate();

        if (!form.isSendReminderTaskActive()) {
            if (task.isActive()) {
                task.setActive(false);

                int canceledCount = reminderEmailOutboxRepository.deleteDispatchableBySourceTaskId(task.getId());

                activityPublisher.publishSuccess(ActivityType.ADMIN_SETTINGS_CHANGED, "SendReminderTask", String.valueOf(task.getId()), "Reminder task disabled");

                if (canceledCount > 0) {
                    activityPublisher.publishSuccess(ActivityType.REMINDER_EMAIL_JOBS_CANCELED, "ReminderEmailOutbox", String.valueOf(task.getId()), "Cancelled " + canceledCount + " pending reminder email job(s) after admin disabled reminders");
                }
            }
            return;
        }

        Instant startSendingTime = createReminderStartInstant(form);

        LocalDate finalSubmissionTime = createFinalSubmissionDate(form);

        validateReminderConfigurationForPersistence(form, startSendingTime, finalSubmissionTime, now);

        task.setActive(true);
        task.setStartSendingTime(startSendingTime);
        task.setRepetitions(form.getReminderRepetitions());
        task.setFrequencyInDays(form.getReminderSendingFrequencyInDays());
        task.setFinalRequestSubmissionDate(finalSubmissionTime);
        task.setCounter(0);

        if (task.getCreationTime() == null) {
            task.setCreationTime(now);
        }

        activityPublisher.publishSuccess(ActivityType.ADMIN_SETTINGS_CHANGED, "SendReminderTask", String.valueOf(task.getId()), "Reminder task configuration updated");
    }

    public boolean hasDayError(SendReminderTaskForm form) {
        Objects.requireNonNull(form, "form must not be null");

        return form.isSendReminderTaskActive() && form.getStartSendingRemindersDay() > 0 && form.getFinalSubmissionDay() > 0 && form.getStartSendingRemindersDay() >= form.getFinalSubmissionDay();
    }

    public boolean isFirstReminderInFuture(SendReminderTaskForm form, Instant now) {
        Objects.requireNonNull(form, "form must not be null");

        if (!form.isSendReminderTaskActive()) {
            return true;
        }

        try {
            return createReminderStartInstant(form).isAfter(now);
        } catch (DateTimeException exception) {
            return false;
        }
    }

    public boolean isSendRemindersSetupValid(SendReminderTaskForm form, Instant now) {
        Objects.requireNonNull(form, "form must not be null");
        Objects.requireNonNull(now, "now must not be null");

        if (!form.isSendReminderTaskActive()) {
            return true;
        }

        int frequencyInDays = form.getReminderSendingFrequencyInDays();
        int repetitions = form.getReminderRepetitions();

        if (repetitions <= 0) {
            return false;
        }

        if (repetitions > 1 && frequencyInDays <= 0) {
            return false;
        }

        if (frequencyInDays > 0 && repetitions <= 1) {
            return false;
        }

        try {
            Instant startSendingTime = createReminderStartInstant(form);
            LocalDate finalSubmissionDate = createFinalSubmissionDate(form);
            Instant finalSubmissionDeadline = finalSubmissionDate.atTime(LocalTime.MAX).atZone(applicationZoneId).toInstant();

            if (!finalSubmissionDeadline.isAfter(now)) {
                return false;
            }

            if (!startSendingTime.isBefore(finalSubmissionDeadline)) {
                return false;
            }

            long daysUntilLastReminder = repetitions == 1 ? 0 : Math.multiplyExact(repetitions - 1L, (long) frequencyInDays);

            Instant lastReminder = startSendingTime.atZone(applicationZoneId).plusDays(daysUntilLastReminder).toInstant();
            YearMonth month = YearMonth.from(now.atZone(applicationZoneId));
            Instant endOfCurrentMonth = month.atEndOfMonth().atTime(LocalTime.MAX).atZone(applicationZoneId).toInstant();

            return !lastReminder.isAfter(endOfCurrentMonth) && lastReminder.isBefore(finalSubmissionDeadline);

        } catch (DateTimeException | ArithmeticException exception) {
            return false;
        }
    }

    public boolean isCleanupTimeInFuture(CleanupTaskForm form, Instant now) {
        Objects.requireNonNull(form, "form must not be null");
        Objects.requireNonNull(now, "now must not be null");

        if (!form.isCleanupTaskActive()) {
            return true;
        }

        try {
            return createCleanupInstant(form).isAfter(now);
        } catch (DateTimeException exception) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public CleanupTaskForm getCleanupTaskForm() {
        CleanupTask task = getCleanupTask();

        CleanupTaskForm form = new CleanupTaskForm();

        if (task.isActive() && task.getExecutionTime() != null) {
            Instant cleanupTime = task.getExecutionTime();

            ZonedDateTime zonedDateTime = cleanupTime.atZone(applicationZoneId);

            form.setCleanupTaskActive(true);
            form.setCleanupDay(zonedDateTime.getDayOfMonth());
            form.setCleanupHour(zonedDateTime.getHour());
            form.setCleanupMinute(zonedDateTime.getMinute());
        }

        return form;
    }

    @Transactional(readOnly = true)
    public SendReminderTaskForm getSendReminderTaskForm() {
        SendReminderTask task = getSendReminderTask();

        SendReminderTaskForm form = new SendReminderTaskForm();

        if (task.isActive() && task.getStartSendingTime() != null && task.getFinalRequestSubmissionDate() != null) {

            form.setSendReminderTaskActive(true);

            Instant startSendingTime = task.getStartSendingTime();
            int startSendingDay = startSendingTime.atZone(applicationZoneId).getDayOfMonth();
            int startSendingHour = startSendingTime.atZone(applicationZoneId).getHour();
            int startSendingMinute = startSendingTime.atZone(applicationZoneId).getMinute();

            form.setStartSendingRemindersDay(startSendingDay);
            form.setStartSendingRemindersHour(startSendingHour);
            form.setStartSendingRemindersMinute(startSendingMinute);
            form.setReminderSendingFrequencyInDays(task.getFrequencyInDays());
            form.setReminderRepetitions(task.getRepetitions());
            form.setFinalSubmissionDay(task.getFinalRequestSubmissionDate().getDayOfMonth());
        }

        return form;
    }

    @Transactional(readOnly = true)
    public CleanupTaskRecord getCleanupTaskRecord() {
        CleanupTask task = getCleanupTask();

        if (!task.isActive() || task.getExecutionTime() == null) {
            return new CleanupTaskRecord(false, null);
        }

        return plannedTaskMapper.entityToCleanupTaskRecord(task);
    }

    @Transactional(readOnly = true)
    public SendReminderTaskRecord getSendReminderTaskRecord() {
        SendReminderTask task = getSendReminderTask();

        if (!task.isActive() || task.getStartSendingTime() == null || task.getFinalRequestSubmissionDate() == null) {
            return new SendReminderTaskRecord(false, 0, 0, null, 0);
        }

        return plannedTaskMapper.entityToSendReminderTaskRecord(task);
    }

    private void validateReminderConfigurationForPersistence(SendReminderTaskForm form, Instant startSendingTime, LocalDate finalSubmissionTime, Instant now) {
        if (!startSendingTime.isAfter(now)) {
            throw new IllegalArgumentException("The first reminder must be in the future");
        }

        Instant finalSubmissionTimeInstant = finalSubmissionTime.atTime(LocalTime.MAX).atZone(applicationZoneId).toInstant();

        if (!finalSubmissionTimeInstant.isAfter(now)) {
            throw new IllegalArgumentException("The final submission deadline must be in the future");
        }

        if (!startSendingTime.isBefore(finalSubmissionTimeInstant)) {
            throw new IllegalArgumentException("The reminder start time must be before the final submission deadline");
        }

        if (!isSendRemindersSetupValid(form, now)) {
            throw new IllegalArgumentException("Reminder occurrences must remain in the current month and before the deadline");
        }
    }

    private Instant createCleanupInstant(CleanupTaskForm form) {
        YearMonth month = YearMonth.now(applicationClock);

        LocalDateTime localDateTime = LocalDateTime.of(month.getYear(), month.getMonth(), form.getCleanupDay(), form.getCleanupHour(), form.getCleanupMinute());

        return localDateTime.atZone(applicationZoneId).toInstant();
    }

    private Instant createReminderStartInstant(SendReminderTaskForm form) {
        YearMonth month = YearMonth.now(applicationClock);

        LocalDateTime localDateTime = LocalDateTime.of(month.getYear(), month.getMonth(), form.getStartSendingRemindersDay(), form.getStartSendingRemindersHour(), form.getStartSendingRemindersMinute());

        return localDateTime.atZone(applicationZoneId).toInstant();
    }

    private LocalDate createFinalSubmissionDate(SendReminderTaskForm form) {
        YearMonth month = YearMonth.now(applicationClock);

        return month.atDay(form.getFinalSubmissionDay());
    }

    /**
     * Must obtain the singleton row using PESSIMISTIC_WRITE.
     * <p>
     * Concurrent administrators must serialize updates.
     **/
    private CleanupTask getCleanupTaskForUpdate() {
        return cleanupTaskRepository.findByIdForUpdate(CleanupTask.SINGLETON_ID).orElseThrow(() -> missingSingleton("cleanup_task", CleanupTask.SINGLETON_ID));
    }

    /**
     * Must obtain the singleton row using PESSIMISTIC_WRITE.
     * <p>
     * Concurrent administrators must serialize updates.
     **/
    private SendReminderTask getSendReminderTaskForUpdate() {
        return sendReminderTaskRepository.findByIdForUpdate(SendReminderTask.SINGLETON_ID).orElseThrow(() -> missingSingleton("send_reminder_task", SendReminderTask.SINGLETON_ID));
    }

    private CleanupTask getCleanupTask() {
        return cleanupTaskRepository.findById(CleanupTask.SINGLETON_ID).orElseThrow(() -> missingSingleton("cleanup_task", CleanupTask.SINGLETON_ID));
    }

    private SendReminderTask getSendReminderTask() {
        return sendReminderTaskRepository.findById(SendReminderTask.SINGLETON_ID).orElseThrow(() -> missingSingleton("send_reminder_task", SendReminderTask.SINGLETON_ID));
    }

    private IllegalStateException missingSingleton(String tableName, Long id) {
        return new IllegalStateException("Required singleton row with ID " + id + " is missing from " + tableName);
    }
}