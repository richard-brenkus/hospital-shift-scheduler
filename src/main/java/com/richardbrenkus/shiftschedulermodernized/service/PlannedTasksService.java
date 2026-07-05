package com.richardbrenkus.shiftschedulermodernized.service;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlannedTasksService {

    private final CleanupTaskRepository cleanupTaskRepository;
    private final SendReminderTaskRepository sendReminderTaskRepository;
    private final PlannedTaskMapper plannedTaskMapper;

    public void saveCleanupTask(CleanupTaskForm cleanupTaskForm) {

        List<CleanupTask> cleanupTaskList = cleanupTaskRepository.findByIsActive();

        CleanupTask cleanupTask = cleanupTaskList.stream().min(Comparator.comparing(CleanupTask::getCreationTime)).orElse(new CleanupTask());

        LocalDateTime cleanupDateTime = LocalDateTime.of(LocalDateTime.now().getYear(), LocalDateTime.now().getMonth(), cleanupTaskForm.getCleanupDay(), cleanupTaskForm.getCleanupHour(), cleanupTaskForm.getCleanupMinute());

        cleanupTask.setExecutionTime(cleanupDateTime);
        cleanupTask.setActive(true);
        cleanupTask.setCreationTime(LocalDateTime.now());
        cleanupTaskRepository.save(cleanupTask);
    }

    public void saveSendReminderTask(SendReminderTaskForm sendReminderTaskForm) {

        List<SendReminderTask> sendReminderTaskList = sendReminderTaskRepository.findByIsActive();
        SendReminderTask sendReminderTask = sendReminderTaskList.stream().min(Comparator.comparing(SendReminderTask::getCreationTime)).orElse(new SendReminderTask());

        LocalDateTime startSendingTime = LocalDateTime.of(LocalDateTime.now().getYear(), LocalDateTime.now().getMonth(), sendReminderTaskForm.getStartSendingRemindersDay(), sendReminderTaskForm.getStartSendingRemindersHour(), sendReminderTaskForm.getStartSendingRemindersMinute());

        sendReminderTask.setStartSendingTime(startSendingTime);
        sendReminderTask.setActive(true);
        sendReminderTask.setRepetitions(sendReminderTaskForm.getReminderRepetitions());
        sendReminderTask.setFrequencyInDays(sendReminderTaskForm.getReminderSendingFrequencyInDays());

        sendReminderTaskRepository.save(sendReminderTask);
    }

    public boolean hasDayError(SendReminderTaskForm sendReminderTaskForm) {

        return sendReminderTaskForm.getStartSendingRemindersDay() != 0 && sendReminderTaskForm.getStartSendingRemindersDay() >= sendReminderTaskForm.getFinalSubmissionDay();
    }

    public CleanupTaskForm getCleanupTaskForm() {

        CleanupTaskForm cleanupTaskForm = new CleanupTaskForm();

        CleanupTask cleanupTask = getCleanupTask();

        if (cleanupTask.isActive()) {
            cleanupTaskForm.setCleanupTaskActive(true);
            cleanupTaskForm.setCleanupDay(cleanupTask.getExecutionTime().getDayOfMonth());
            cleanupTaskForm.setCleanupHour(cleanupTask.getExecutionTime().getHour());
            cleanupTaskForm.setCleanupMinute(cleanupTask.getExecutionTime().getMinute());
        }

        return cleanupTaskForm;
    }

    public SendReminderTaskForm getSendReminderTaskForm() {

        SendReminderTaskForm sendReminderTaskForm = new SendReminderTaskForm();

        SendReminderTask sendReminderTask = getSendReminderTask();


        if (sendReminderTask.isActive()) {
            sendReminderTaskForm.setSendReminderTaskActive(true);
            sendReminderTaskForm.setReminderRepetitions(sendReminderTask.getRepetitions());
            sendReminderTaskForm.setReminderSendingFrequencyInDays(sendReminderTask.getFrequencyInDays());
            sendReminderTaskForm.setStartSendingRemindersDay(sendReminderTask.getStartSendingTime().getDayOfMonth());
            sendReminderTaskForm.setStartSendingRemindersHour(sendReminderTask.getStartSendingTime().getHour());
            sendReminderTaskForm.setStartSendingRemindersMinute(sendReminderTask.getStartSendingTime().getMinute());
        }

        return sendReminderTaskForm;
    }

    public CleanupTaskRecord getCleanupTaskRecord() {

        CleanupTask cleanupTask = getCleanupTask();

        return plannedTaskMapper.entityToCleanupTaskRecord(cleanupTask);
    }

    public SendReminderTaskRecord getSendReminderTaskRecord() {

        SendReminderTask sendReminderTask = getSendReminderTask();

        return plannedTaskMapper.entityToSendReminderTaskRecord(sendReminderTask);
    }

    private CleanupTask getCleanupTask() {

        List<CleanupTask> cleanupTaskList = cleanupTaskRepository.findByIsActive();

        return cleanupTaskList.stream().min(Comparator.comparing(CleanupTask::getCreationTime)).orElse(new CleanupTask());
    }

    private SendReminderTask getSendReminderTask() {
        List<SendReminderTask> sendReminderTaskList = sendReminderTaskRepository.findByIsActive();

        return sendReminderTaskList.stream().min(Comparator.comparing(SendReminderTask::getCreationTime)).orElse(new SendReminderTask());
    }

    public int returnVerifiedDay(int day, LocalDate localDate, ZoneId zoneId) {

        int resultingDay = day;

        if (localDate.isLeapYear()) {
            if (day > ZonedDateTime.now(zoneId).getMonth().maxLength())
                resultingDay = ZonedDateTime.now(zoneId).getMonth().maxLength();
        }
        if (!localDate.isLeapYear()) {
            if (day > ZonedDateTime.now(zoneId).getMonth().minLength())
                resultingDay = ZonedDateTime.now(zoneId).getMonth().minLength();
        }

        return resultingDay;
    }
}
