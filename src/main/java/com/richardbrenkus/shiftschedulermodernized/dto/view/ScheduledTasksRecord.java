package com.richardbrenkus.shiftschedulermodernized.dto.view;

import com.richardbrenkus.shiftschedulermodernized.entity.ScheduledEventsProfile;

public record ScheduledTasksRecord(String reminderTaskInfo, String cleanupTaskInfo, boolean reminderIsActive, boolean cleanupIsActive, int reminderRepetitions, int reminderFrequency, String reminderStart, String reminderDeadline, String cleanupDateTime, ScheduledEventsProfile reminderTask, ScheduledEventsProfile cleanupTask) {

}
