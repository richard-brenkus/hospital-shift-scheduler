package com.richardbrenkus.shiftschedulermodernized.dto.view;

import java.time.LocalDateTime;

public record SendReminderTaskRecord(
        boolean reminderIsActive,
        int reminderRepetitions,
        int reminderFrequency,
        LocalDateTime reminderStart,
        int reminderFinalSubmissionDay
) {
}
