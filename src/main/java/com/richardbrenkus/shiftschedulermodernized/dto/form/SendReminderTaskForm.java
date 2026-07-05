package com.richardbrenkus.shiftschedulermodernized.dto.form;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendReminderTaskForm {

    private boolean isSendReminderTaskActive;
    private LocalDateTime startSendingReminders;
    private int startSendingRemindersDay;
    private int startSendingRemindersHour;
    private int startSendingRemindersMinute;
    private int reminderSendingFrequencyInDays;
    private int reminderRepetitions;
    private int finalSubmissionDay;
}
