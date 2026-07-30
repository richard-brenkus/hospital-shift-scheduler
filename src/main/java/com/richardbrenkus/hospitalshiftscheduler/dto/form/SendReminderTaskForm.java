package com.richardbrenkus.hospitalshiftscheduler.dto.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendReminderTaskForm {

    private boolean isSendReminderTaskActive;

    @Min(1)
    @Max(31)
    private int startSendingRemindersDay;

    @Min(0)
    @Max(23)
    private int startSendingRemindersHour;

    @Min(0)
    @Max(59)
    private int startSendingRemindersMinute;

    @Min(0)
    @Max(10)
    private int reminderSendingFrequencyInDays;

    @Min(1)
    @Max(10)
    private int reminderRepetitions;

    @Min(1)
    @Max(31)
    private int finalSubmissionDay;
}
