package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.ApplicationConstants;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ScheduledEvent;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ScheduledTasksRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.ScheduledEventsProfile;
import com.richardbrenkus.shiftschedulermodernized.repository.ScheduledEventsProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class ScheduledTasksService {

    private final ScheduledEventsProfileRepository scheduledEventsRepository;
    private final MonthlyScheduleService monthlyScheduleService;


    public ScheduledTasksService(ScheduledEventsProfileRepository scheduledEventsRepository, MonthlyScheduleService monthlyScheduleService) {
        this.scheduledEventsRepository = scheduledEventsRepository;
        this.monthlyScheduleService = monthlyScheduleService;
    }

    public ScheduledTasksRecord getScheduledTasksRecord() {

        ZoneId zoneId = ApplicationConstants.ZONE_ID;
        int year = ZonedDateTime.now(zoneId).getYear();
        int month = ZonedDateTime.now(zoneId).getMonthValue();

        String reminderTaskInfo = "Email reminders to submit a request are currently not scheduled.";
        String cleanupTaskInfo = "Request cleanup is currently not scheduled.";
        boolean reminderIsActive = false;
        boolean cleanupIsActive = false;
        int reminderRepetitions = 0;
        int reminderFrequency = 0;
        String cleanupDateTime = "";
        String reminderStart = "";
        String reminderDeadline = "";

        ScheduledEventsProfile reminderTask = scheduledEventsRepository.selectByTaskType(ScheduledEvent.EMAIL_REMINDER.toString());
        if (reminderTask != null) {
            String frequencyString = ", send only once";
            if (reminderTask.getRepetitions() > 1) {
                if (reminderTask.getFrequencyInDays() == 1) {
                    frequencyString = ", send every day";
                }
                if (reminderTask.getFrequencyInDays() > 1) {
                    frequencyString = ", send every " + reminderTask.getFrequencyInDays() + " days";
                }
            }
            if (reminderTask.isTaskActive()) {

                addScheduledTasksFinalSubmissionDay(zoneId, year, month, reminderTask);

                reminderIsActive = true;
                reminderRepetitions = reminderTask.getRepetitions();
                reminderFrequency = reminderTask.getFrequencyInDays();
                reminderStart = reminderTask.getYearMonthDayHourMinuteCode();
                reminderDeadline = reminderTask.getFinalSubmissionYearMonthDayHourMinuteCode();

                reminderTaskInfo = "Scheduled email reminders: " + reminderTask.getYearMonthDayHourMinuteCode() + ", repetitons: " + reminderTask.getRepetitions() + frequencyString + ", submission deadline: " + reminderTask.getFinalSubmissionYearMonthDayHourMinuteCode();
            }
        }

        ScheduledEventsProfile cleanupTask = scheduledEventsRepository.selectByTaskType(ScheduledEvent.REQUEST_CLEANUP.toString());
        if (cleanupTask != null) {
            if (cleanupTask.isTaskActive()) {
                addScheduledTasksFinalSubmissionDay(zoneId, year, month, cleanupTask);
                cleanupIsActive = true;
                cleanupDateTime = cleanupTask.getYearMonthDayHourMinuteCode();
                cleanupTaskInfo = "Request cleanup: " + cleanupTask.getYearMonthDayHourMinuteCode();
            }
        }

        return new ScheduledTasksRecord(reminderTaskInfo, cleanupTaskInfo, reminderIsActive, cleanupIsActive, reminderRepetitions, reminderFrequency, reminderStart, reminderDeadline, cleanupDateTime, reminderTask, cleanupTask);
    }

    public void addScheduledTasksFinalSubmissionDay(ZoneId zoneId, int year, int month, ScheduledEventsProfile reminderTask) {
        int day = reminderTask.getDay();
        int finalDay = reminderTask.getFinalSubmissionDay();
        reminderTask.setYear(year);
        reminderTask.setMonth(month);
        LocalDate localDate = LocalDate.now(zoneId);
        day = monthlyScheduleService.returnVerifiedDay(day, localDate, zoneId);
        finalDay = monthlyScheduleService.returnVerifiedDay(finalDay, localDate, zoneId);

        reminderTask.setDay(day);
        reminderTask.setFinalSubmissionDay(finalDay);
    }
}
