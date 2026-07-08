package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.dto.form.CleanupTaskForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.SendReminderTaskForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.CleanupTaskRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.SendReminderTaskRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.CleanupTask;
import com.richardbrenkus.shiftschedulermodernized.entity.SendReminderTask;
import com.richardbrenkus.shiftschedulermodernized.repository.CleanupTaskRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.SendReminderTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Transactional
public class PlannedTasksService {

    private final CleanupTaskRepository cleanupTaskRepository;
    private final SendReminderTaskRepository sendReminderTaskRepository;

    public void saveCleanupTask(CleanupTaskForm form) {

        if (!form.isCleanupTaskActive()) {
            deactivateAllCleanupTasks();
            return;
        }

        CleanupTask task = getActiveCleanupTaskOrNull();

        if (task == null) {
            task = getExistingCleanupTaskOrNew();
        }

        deactivateAllCleanupTasks();

        task.setActive(true);
        task.setExecutionTime(toTodayOrTomorrowDateTime(
                form.getCleanupDay(),
                form.getCleanupHour(),
                form.getCleanupMinute()
        ));

        if (task.getCreationTime() == null) {
            task.setCreationTime(LocalDateTime.now());
        }

        cleanupTaskRepository.save(task);
    }

    public void saveSendReminderTask(SendReminderTaskForm form) {

        if (!form.isSendReminderTaskActive()) {
            deactivateAllSendReminderTasks();
            return;
        }

        SendReminderTask task = getActiveSendReminderTaskOrNull();

        if (task == null) {
            task = getExistingSendReminderTaskOrNew();
        }

        deactivateAllSendReminderTasks();

        task.setActive(true);
        task.setStartSendingTime(toTodayOrTomorrowDateTime(
                form.getStartSendingRemindersDay(),
                form.getStartSendingRemindersHour(),
                form.getStartSendingRemindersMinute()
        ));
        task.setRepetitions(form.getReminderRepetitions());
        task.setFrequencyInDays(form.getReminderSendingFrequencyInDays());
        task.setFinalSubmissionDay(form.getFinalSubmissionDay());

        if (task.getCreationTime() == null) {
            task.setCreationTime(LocalDateTime.now());
        }

        sendReminderTaskRepository.save(task);
    }

    public boolean hasDayError(SendReminderTaskForm form) {
        return form.isSendReminderTaskActive()
                && form.getStartSendingRemindersDay() != 0
                && form.getStartSendingRemindersDay() >= form.getFinalSubmissionDay();
    }

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

    public SendReminderTaskForm getSendReminderTaskForm() {
        SendReminderTaskForm form = new SendReminderTaskForm();

        SendReminderTask task = getActiveSendReminderTaskOrNull();

        if (task != null && task.getStartSendingTime() != null) {
            form.setSendReminderTaskActive(true);
            form.setStartSendingRemindersDay(task.getStartSendingTime().getDayOfMonth());
            form.setStartSendingRemindersHour(task.getStartSendingTime().getHour());
            form.setStartSendingRemindersMinute(task.getStartSendingTime().getMinute());
            form.setReminderSendingFrequencyInDays(task.getFrequencyInDays());
            form.setReminderRepetitions(task.getRepetitions());
            form.setFinalSubmissionDay(task.getFinalSubmissionDay());
        }

        return form;
    }

    public CleanupTaskRecord getCleanupTaskRecord() {
        CleanupTask task = getActiveCleanupTaskOrNull();

        if (task == null || task.getExecutionTime() == null) {
            return new CleanupTaskRecord(false, null);
        }

        return new CleanupTaskRecord(true, task.getExecutionTime());
    }

    public SendReminderTaskRecord getSendReminderTaskRecord() {
        SendReminderTask task = getActiveSendReminderTaskOrNull();

        if (task == null || task.getStartSendingTime() == null) {
            return new SendReminderTaskRecord(false, 0, 0, null, 0);
        }

        return new SendReminderTaskRecord(
                true,
                task.getRepetitions(),
                task.getFrequencyInDays(),
                task.getStartSendingTime(),
                task.getFinalSubmissionDay()
        );
    }

    private void deactivateAllCleanupTasks() {
        cleanupTaskRepository.findAll().forEach(task -> {
            if (task.isActive()) {
                task.setActive(false);
                cleanupTaskRepository.save(task);
            }
        });
    }

    private void deactivateAllSendReminderTasks() {
        sendReminderTaskRepository.findAll().forEach(task -> {
            if (task.isActive()) {
                task.setActive(false);
                sendReminderTaskRepository.save(task);
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

    private LocalDateTime toTodayOrTomorrowDateTime(int day, int hour, int minute) {
        LocalDateTime requestedDate = LocalDateTime.of(LocalDateTime.now().getYear(), LocalDateTime.now().getMonth(), day, hour, minute);
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(requestedDate)
                ? requestedDate
                : now.plusDays(1).withHour(hour).withMinute(minute);
    }
}
