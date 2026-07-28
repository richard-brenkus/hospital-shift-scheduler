package com.richardbrenkus.shiftschedulermodernized.dto.view;

import java.time.ZonedDateTime;

public record SendReminderTaskRecord(
        boolean reminderIsActive,
        int reminderRepetitions,
        int reminderFrequency,
        ZonedDateTime reminderStart,
        int reminderFinalSubmissionDay
) {
}
