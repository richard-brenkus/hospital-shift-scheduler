package com.richardbrenkus.shiftschedulermodernized.dto.view;

import java.time.Instant;

public record SendReminderTaskRecord(
        boolean reminderIsActive,
        int reminderRepetitions,
        int reminderFrequency,
        Instant reminderStart,
        int reminderFinalSubmissionDay
) {
}
