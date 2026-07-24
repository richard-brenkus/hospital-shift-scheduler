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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PlannedTasksService {

    private final CleanupTaskRepository cleanupTaskRepository;
    private final SendReminderTaskRepository sendReminderTaskRepository;
    private final ActivityPublisher activityPublisher;
    private final PlannedTaskMapper plannedTaskMapper;
    private final Clock applicationClock;

    @Transactional
    public void saveCleanupTask(
            CleanupTaskForm form, LocalDateTime now
    ) {
        Objects.requireNonNull(
                form,
                "form must not be null"
        );

        CleanupTask task = getCleanupTaskForUpdate();

        if (!form.isCleanupTaskActive()) {
            if (task.isActive()) {
                task.setActive(false);

                activityPublisher.publishSuccess(
                        ActivityType.ADMIN_SETTINGS_CHANGED,
                        "CleanupTask",
                        String.valueOf(task.getId()),
                        "Cleanup task disabled"
                );
            }
            return;
        }

        LocalDateTime executionTime =
                createCleanupDateTime(form, now);

        if (!executionTime.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Cleanup time must be in the future"
            );
        }

        task.setActive(true);
        task.setExecutionTime(executionTime);

        if (task.getCreationTime() == null) {
            task.setCreationTime(now);
        }

        activityPublisher.publishSuccess(
                ActivityType.ADMIN_SETTINGS_CHANGED,
                "CleanupTask",
                String.valueOf(task.getId()),
                "Cleanup task configuration updated"
        );
    }

    @Transactional
    public void saveSendReminderTask(SendReminderTaskForm form, LocalDateTime now) {
        Objects.requireNonNull(form, "form must not be null");
        Objects.requireNonNull(now, "now must not be null");

        SendReminderTask task = getSendReminderTaskForUpdate();

        if (!form.isSendReminderTaskActive()) {
            if (task.isActive()) {
                task.setActive(false);

                activityPublisher.publishSuccess(
                        ActivityType.ADMIN_SETTINGS_CHANGED,
                        "SendReminderTask",
                        String.valueOf(task.getId()),
                        "Reminder task disabled"
                );
            }
            return;
        }

        LocalDateTime startSendingTime = createReminderStartDateTime(form, now);

        LocalDateTime finalSubmissionTime = createFinalSubmissionDateTime(form, now);

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

        activityPublisher.publishSuccess(
                ActivityType.ADMIN_SETTINGS_CHANGED,
                "SendReminderTask",
                String.valueOf(task.getId()),
                "Reminder task configuration updated"
        );
    }

    public boolean hasDayError(
            SendReminderTaskForm form
    ) {
        Objects.requireNonNull(
                form,
                "form must not be null"
        );

        return form.isSendReminderTaskActive()
                && form.getStartSendingRemindersDay() > 0
                && form.getFinalSubmissionDay() > 0
                && form.getStartSendingRemindersDay()
                >= form.getFinalSubmissionDay();
    }

    public boolean isFirstReminderInFuture(
            SendReminderTaskForm form, LocalDateTime now
    ) {
        Objects.requireNonNull(
                form,
                "form must not be null"
        );

        if (!form.isSendReminderTaskActive()) {
            return true;
        }

        try {
            return createReminderStartDateTime(form, now)
                    .isAfter(now);
        } catch (DateTimeException exception) {
            return false;
        }
    }

    public boolean isSendRemindersSetupValid(
            SendReminderTaskForm form, LocalDateTime now
    ) {
        Objects.requireNonNull(
                form,
                "form must not be null"
        );

        if (!form.isSendReminderTaskActive()) {
            return true;
        }

        int repetitions =
                form.getReminderRepetitions();

        int frequencyInDays =
                form.getReminderSendingFrequencyInDays();

        if (repetitions <= 0) {
            return false;
        }

        if (repetitions > 1
                && frequencyInDays <= 0) {
            return false;
        }

        try {
            LocalDateTime startSendingTime =
                    createReminderStartDateTime(form, now);

            LocalDateTime finalSubmissionTime =
                    createFinalSubmissionDateTime(form, now);

            if (!finalSubmissionTime.isAfter(now)) {
                return false;
            }

            if (!startSendingTime
                    .isBefore(finalSubmissionTime)) {
                return false;
            }

            long daysUntilLastReminder =
                    repetitions == 1
                            ? 0
                            : Math.multiplyExact(
                            repetitions - 1L,
                            (long) frequencyInDays
                    );

            LocalDateTime lastReminder =
                    startSendingTime.plusDays(
                            daysUntilLastReminder
                    );

            LocalDateTime endOfCurrentMonth =
                    LocalDateTime.of(
                            YearMonth.from(now).atEndOfMonth(),
                            LocalTime.MAX
                    );

            return !lastReminder
                    .isAfter(endOfCurrentMonth)
                    && lastReminder
                    .isBefore(finalSubmissionTime);

        } catch (DateTimeException
                 | ArithmeticException exception) {
            return false;
        }
    }

    public boolean isCleanupTimeInFuture(
            CleanupTaskForm form, LocalDateTime now
    ) {
        Objects.requireNonNull(
                form,
                "form must not be null"
        );

        if (!form.isCleanupTaskActive()) {
            return true;
        }

        try {
            return createCleanupDateTime(form, now)
                    .isAfter(now);
        } catch (DateTimeException exception) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public CleanupTaskForm getCleanupTaskForm() {
        CleanupTask task = getCleanupTask();

        CleanupTaskForm form =
                new CleanupTaskForm();

        if (task.isActive()
                && task.getExecutionTime() != null) {
            form.setCleanupTaskActive(true);
            form.setCleanupDay(
                    task.getExecutionTime()
                            .getDayOfMonth()
            );
            form.setCleanupHour(
                    task.getExecutionTime().getHour()
            );
            form.setCleanupMinute(
                    task.getExecutionTime().getMinute()
            );
        }

        return form;
    }

    @Transactional(readOnly = true)
    public SendReminderTaskForm getSendReminderTaskForm() {
        SendReminderTask task =
                getSendReminderTask();

        SendReminderTaskForm form =
                new SendReminderTaskForm();

        if (task.isActive()
                && task.getStartSendingTime() != null
                && task.getFinalRequestSubmissionDate()
                != null) {

            form.setSendReminderTaskActive(true);
            form.setStartSendingRemindersDay(
                    task.getStartSendingTime()
                            .getDayOfMonth()
            );
            form.setStartSendingRemindersHour(
                    task.getStartSendingTime()
                            .getHour()
            );
            form.setStartSendingRemindersMinute(
                    task.getStartSendingTime()
                            .getMinute()
            );
            form.setReminderSendingFrequencyInDays(
                    task.getFrequencyInDays()
            );
            form.setReminderRepetitions(
                    task.getRepetitions()
            );
            form.setFinalSubmissionDay(
                    task.getFinalRequestSubmissionDate()
                            .getDayOfMonth()
            );
        }

        return form;
    }

    @Transactional(readOnly = true)
    public CleanupTaskRecord getCleanupTaskRecord() {
        CleanupTask task = getCleanupTask();

        if (!task.isActive()
                || task.getExecutionTime() == null) {
            return new CleanupTaskRecord(
                    false,
                    null
            );
        }

        return plannedTaskMapper
                .entityToCleanupTaskRecord(task);
    }

    @Transactional(readOnly = true)
    public SendReminderTaskRecord getSendReminderTaskRecord() {
        SendReminderTask task =
                getSendReminderTask();

        if (!task.isActive()
                || task.getStartSendingTime() == null
                || task.getFinalRequestSubmissionDate()
                == null) {
            return new SendReminderTaskRecord(
                    false,
                    0,
                    0,
                    null,
                    0
            );
        }

        return plannedTaskMapper
                .entityToSendReminderTaskRecord(task);
    }

    private void validateReminderConfigurationForPersistence(
            SendReminderTaskForm form,
            LocalDateTime startSendingTime,
            LocalDateTime finalSubmissionTime,
            LocalDateTime now
    ) {
        if (!startSendingTime.isAfter(now)) {
            throw new IllegalArgumentException(
                    "The first reminder must be in the future"
            );
        }

        if (!finalSubmissionTime.isAfter(now)) {
            throw new IllegalArgumentException(
                    "The final submission deadline must be "
                            + "in the future"
            );
        }

        if (!startSendingTime
                .isBefore(finalSubmissionTime)) {
            throw new IllegalArgumentException(
                    "The reminder start time must be before "
                            + "the final submission deadline"
            );
        }

        if (!isSendRemindersSetupValid(form, now)) {
            throw new IllegalArgumentException(
                    "Reminder occurrences must remain in "
                            + "the current month and before "
                            + "the deadline"
            );
        }
    }

    private LocalDateTime createCleanupDateTime(
            CleanupTaskForm form, LocalDateTime now
    ) {
        YearMonth month = YearMonth.from(now);

        return LocalDateTime.of(
                month.getYear(),
                month.getMonth(),
                form.getCleanupDay(),
                form.getCleanupHour(),
                form.getCleanupMinute()
        );
    }

    private LocalDateTime createReminderStartDateTime(
            SendReminderTaskForm form, LocalDateTime now
    ) {
        YearMonth month = YearMonth.from(now);

        return LocalDateTime.of(
                month.getYear(),
                month.getMonth(),
                form.getStartSendingRemindersDay(),
                form.getStartSendingRemindersHour(),
                form.getStartSendingRemindersMinute()
        );
    }

    private LocalDateTime createFinalSubmissionDateTime(
            SendReminderTaskForm form, LocalDateTime now
    ) {
        YearMonth month = YearMonth.from(now);

        return LocalDateTime.of(
                month.getYear(),
                month.getMonth(),
                form.getFinalSubmissionDay(),
                23,
                59,
                59
        );
    }

    /**
     * Must obtain the singleton row using PESSIMISTIC_WRITE.
     * <p>
     * Concurrent administrators must serialize updates.
     **/
    private CleanupTask getCleanupTaskForUpdate() {
        return cleanupTaskRepository
                .findByIdForUpdate(CleanupTask.SINGLETON_ID)
                .orElseThrow(() ->
                        missingSingleton(
                                "cleanup_task",
                                CleanupTask.SINGLETON_ID
                        )
                );
    }

    /**
     * Must obtain the singleton row using PESSIMISTIC_WRITE.
     * <p>
     * Concurrent administrators must serialize updates.
     **/
    private SendReminderTask
    getSendReminderTaskForUpdate() {
        return sendReminderTaskRepository
                .findByIdForUpdate(
                        SendReminderTask.SINGLETON_ID
                )
                .orElseThrow(() ->
                        missingSingleton(
                                "send_reminder_task",
                                SendReminderTask.SINGLETON_ID
                        )
                );
    }

    private CleanupTask getCleanupTask() {
        return cleanupTaskRepository
                .findById(CleanupTask.SINGLETON_ID)
                .orElseThrow(() ->
                        missingSingleton(
                                "cleanup_task",
                                CleanupTask.SINGLETON_ID
                        )
                );
    }

    private SendReminderTask getSendReminderTask() {
        return sendReminderTaskRepository
                .findById(
                        SendReminderTask.SINGLETON_ID
                )
                .orElseThrow(() ->
                        missingSingleton(
                                "send_reminder_task",
                                SendReminderTask.SINGLETON_ID
                        )
                );
    }

    private IllegalStateException missingSingleton(
            String tableName,
            Long id
    ) {
        return new IllegalStateException(
                "Required singleton row with ID "
                        + id
                        + " is missing from "
                        + tableName
        );
    }
}