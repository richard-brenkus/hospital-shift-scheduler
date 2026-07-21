package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CleanupTaskForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.SendReminderTaskForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.CleanupTaskRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.SendReminderTaskRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.CleanupTask;
import com.richardbrenkus.shiftschedulermodernized.entity.SendReminderTask;
import com.richardbrenkus.shiftschedulermodernized.mapper.PlannedTaskMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.CleanupTaskRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.SendReminderTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class PlannedTasksService {

    private final CleanupTaskRepository cleanupTaskRepository;
    private final SendReminderTaskRepository sendReminderTaskRepository;
    private final ActivityPublisher activityPublisher;
    private final PlannedTaskMapper plannedTaskMapper;
    private final Clock applicationClock;

    public void saveCleanupTask(CleanupTaskForm form) {
        Objects.requireNonNull(form, "form must not be null");

        if (!form.isCleanupTaskActive()) {
            deactivateAllCleanupTasks();
            return;
        }

        LocalDateTime executionTime = createCleanupDateTime(form);

        if (!executionTime.isAfter(now())) {
            throw new IllegalArgumentException(
                    "Cleanup time must be in the future"
            );
        }

        CleanupTask task = getActiveCleanupTaskOrNull();

        if (task == null) {
            task = getExistingCleanupTaskOrNew();
        }

        deactivateAllCleanupTasks();

        task.setActive(true);
        task.setExecutionTime(executionTime);

        if (task.getCreationTime() == null) {
            task.setCreationTime(now());
        }

        cleanupTaskRepository.saveAndFlush(task);

        activityPublisher.publishSuccess(
                ActivityType.ADMIN_SETTINGS_CHANGED,
                "CleanupTask",
                String.valueOf(task.getId()),
                "Cleanup task configuration updated"
        );
    }

    public void saveSendReminderTask(SendReminderTaskForm form) {
        Objects.requireNonNull(form, "form must not be null");

        if (!form.isSendReminderTaskActive()) {
            deactivateAllSendReminderTasks();
            return;
        }

        LocalDateTime startSendingTime = createReminderStartDateTime(form);
        LocalDateTime finalSubmissionTime = createFinalSubmissionDateTime(form);

        validateReminderConfigurationForPersistence(
                form,
                startSendingTime,
                finalSubmissionTime
        );

        SendReminderTask task = getActiveSendReminderTaskOrNull();

        if (task == null) {
            task = getExistingSendReminderTaskOrNew();
        }

        deactivateAllSendReminderTasks();

        task.setActive(true);
        task.setStartSendingTime(startSendingTime);
        task.setRepetitions(form.getReminderRepetitions());
        task.setFrequencyInDays(form.getReminderSendingFrequencyInDays());
        task.setFinalRequestSubmissionDate(finalSubmissionTime);
        task.setCounter(0);

        if (task.getCreationTime() == null) {
            task.setCreationTime(now());
        }

        sendReminderTaskRepository.saveAndFlush(task);

        activityPublisher.publishSuccess(
                ActivityType.ADMIN_SETTINGS_CHANGED,
                "SendReminderTask",
                String.valueOf(task.getId()),
                "Reminder task configuration updated"
        );
    }

    @Transactional(readOnly = true)
    public boolean hasDayError(SendReminderTaskForm form) {
        Objects.requireNonNull(form, "form must not be null");

        return form.isSendReminderTaskActive()
                && form.getStartSendingRemindersDay() > 0
                && form.getFinalSubmissionDay() > 0
                && form.getStartSendingRemindersDay()
                >= form.getFinalSubmissionDay();
    }

    @Transactional(readOnly = true)
    public boolean isFirstReminderInFuture(SendReminderTaskForm form) {
        Objects.requireNonNull(form, "form must not be null");

        if (!form.isSendReminderTaskActive()) {
            return true;
        }

        try {
            return createReminderStartDateTime(form).isAfter(now());
        } catch (DateTimeException exception) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean isSendRemindersSetupValid(SendReminderTaskForm form) {
        Objects.requireNonNull(form, "form must not be null");

        if (!form.isSendReminderTaskActive()) {
            return true;
        }

        int repetitions = form.getReminderRepetitions();
        int frequencyInDays = form.getReminderSendingFrequencyInDays();

        if (repetitions <= 0 || frequencyInDays <= 0) {
            return false;
        }

        try {
            LocalDateTime startSendingTime = createReminderStartDateTime(form);
            LocalDateTime finalSubmissionTime = createFinalSubmissionDateTime(form);

            long daysUntilLastReminder = Math.multiplyExact(
                    repetitions - 1L,
                    (long) frequencyInDays
            );

            LocalDateTime lastReminder = startSendingTime.plusDays(
                    daysUntilLastReminder
            );

            LocalDateTime endOfCurrentMonth = LocalDateTime.of(
                    currentMonth().atEndOfMonth(),
                    LocalTime.MAX
            );

            return !lastReminder.isAfter(endOfCurrentMonth)
                    && lastReminder.isBefore(finalSubmissionTime);

        } catch (DateTimeException | ArithmeticException exception) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean isCleanupTimeInFuture(CleanupTaskForm form) {
        Objects.requireNonNull(form, "form must not be null");

        if (!form.isCleanupTaskActive()) {
            return true;
        }

        try {
            return createCleanupDateTime(form).isAfter(now());
        } catch (DateTimeException exception) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public CleanupTaskForm getCleanupTaskForm() {
        CleanupTaskForm form = new CleanupTaskForm();
        CleanupTask task = getActiveCleanupTaskOrNull();

        if (task != null && task.getExecutionTime() != null) {
            form.setCleanupTaskActive(true);
            form.setCleanupDay(task.getExecutionTime().getDayOfMonth());
            form.setCleanupHour(task.getExecutionTime().getHour());
            form.setCleanupMinute(task.getExecutionTime().getMinute());
        }

        return form;
    }

    @Transactional(readOnly = true)
    public SendReminderTaskForm getSendReminderTaskForm() {
        SendReminderTaskForm form = new SendReminderTaskForm();
        SendReminderTask task = getActiveSendReminderTaskOrNull();

        if (task != null
                && task.getStartSendingTime() != null
                && task.getFinalRequestSubmissionDate() != null) {
            form.setSendReminderTaskActive(true);
            form.setStartSendingRemindersDay(
                    task.getStartSendingTime().getDayOfMonth()
            );
            form.setStartSendingRemindersHour(
                    task.getStartSendingTime().getHour()
            );
            form.setStartSendingRemindersMinute(
                    task.getStartSendingTime().getMinute()
            );
            form.setReminderSendingFrequencyInDays(task.getFrequencyInDays());
            form.setReminderRepetitions(task.getRepetitions());
            form.setFinalSubmissionDay(
                    task.getFinalRequestSubmissionDate().getDayOfMonth()
            );
        }

        return form;
    }

    @Transactional(readOnly = true)
    public CleanupTaskRecord getCleanupTaskRecord() {
        CleanupTask task = getActiveCleanupTaskOrNull();

        if (task == null || task.getExecutionTime() == null) {
            return new CleanupTaskRecord(false, null);
        }

        return plannedTaskMapper.entityToCleanupTaskRecord(task);
    }

    @Transactional(readOnly = true)
    public SendReminderTaskRecord getSendReminderTaskRecord() {
        SendReminderTask task = getActiveSendReminderTaskOrNull();

        if (task == null
                || task.getStartSendingTime() == null
                || task.getFinalRequestSubmissionDate() == null) {
            return new SendReminderTaskRecord(false, 0, 0, null, 0);
        }

        return plannedTaskMapper.entityToSendReminderTaskRecord(task);
    }

    private void validateReminderConfigurationForPersistence(
            SendReminderTaskForm form,
            LocalDateTime startSendingTime,
            LocalDateTime finalSubmissionTime
    ) {
        if (!startSendingTime.isAfter(now())) {
            throw new IllegalArgumentException(
                    "The first reminder must be in the future"
            );
        }

        if (!startSendingTime.isBefore(finalSubmissionTime)) {
            throw new IllegalArgumentException(
                    "The reminder start time must be before the final submission deadline"
            );
        }

        if (!isSendRemindersSetupValid(form)) {
            throw new IllegalArgumentException(
                    "Reminder occurrences must remain in the current month and before the deadline"
            );
        }
    }

    private LocalDateTime createCleanupDateTime(CleanupTaskForm form) {
        YearMonth month = currentMonth();

        return LocalDateTime.of(
                month.getYear(),
                month.getMonth(),
                form.getCleanupDay(),
                form.getCleanupHour(),
                form.getCleanupMinute()
        );
    }

    private LocalDateTime createReminderStartDateTime(
            SendReminderTaskForm form
    ) {
        YearMonth month = currentMonth();

        return LocalDateTime.of(
                month.getYear(),
                month.getMonth(),
                form.getStartSendingRemindersDay(),
                form.getStartSendingRemindersHour(),
                form.getStartSendingRemindersMinute()
        );
    }

    private LocalDateTime createFinalSubmissionDateTime(
            SendReminderTaskForm form
    ) {
        YearMonth month = currentMonth();

        return LocalDateTime.of(
                month.getYear(),
                month.getMonth(),
                form.getFinalSubmissionDay(),
                23,
                59,
                59
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(applicationClock);
    }

    private YearMonth currentMonth() {
        return YearMonth.from(now());
    }

    private void deactivateAllCleanupTasks() {
        cleanupTaskRepository.findAll().forEach(task -> {
            if (task.isActive()) {
                task.setActive(false);
                cleanupTaskRepository.saveAndFlush(task);

                activityPublisher.publishSuccess(
                        ActivityType.ADMIN_SETTINGS_CHANGED,
                        "CleanupTask",
                        String.valueOf(task.getId()),
                        "Cleanup task disabled"
                );
            }
        });
    }

    private void deactivateAllSendReminderTasks() {
        sendReminderTaskRepository.findAll().forEach(task -> {
            if (task.isActive()) {
                task.setActive(false);
                sendReminderTaskRepository.saveAndFlush(task);

                activityPublisher.publishSuccess(
                        ActivityType.ADMIN_SETTINGS_CHANGED,
                        "SendReminderTask",
                        String.valueOf(task.getId()),
                        "Reminder task disabled"
                );
            }
        });
    }

    private CleanupTask getExistingCleanupTaskOrNew() {
        return cleanupTaskRepository.findAll().stream()
                .min(Comparator.comparing(
                        CleanupTask::getCreationTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .orElse(new CleanupTask());
    }

    private SendReminderTask getExistingSendReminderTaskOrNew() {
        return sendReminderTaskRepository.findAll().stream()
                .min(Comparator.comparing(
                        SendReminderTask::getCreationTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .orElse(new SendReminderTask());
    }

    private CleanupTask getActiveCleanupTaskOrNull() {
        return cleanupTaskRepository.findAll().stream()
                .filter(CleanupTask::isActive)
                .min(Comparator.comparing(
                        CleanupTask::getCreationTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .orElse(null);
    }

    private SendReminderTask getActiveSendReminderTaskOrNull() {
        return sendReminderTaskRepository.findAll().stream()
                .filter(SendReminderTask::isActive)
                .min(Comparator.comparing(
                        SendReminderTask::getCreationTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .orElse(null);
    }
}