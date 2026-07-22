package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.util.CalendarDateIdUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggingEmailReminderServiceTest {

    private final LoggingEmailReminderService service = new LoggingEmailReminderService();

    @Test
    void shouldNotThrow_whenSendingReminder() {
        assertThatCode(() -> service.sendShiftRequestReminderEmails(CalendarDateIdUtils.returnAdjustedFinalSubmissionDateTime(20).toLocalDate(), "test-idempotentKey-1"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrow_whenSubmissionDayIsZero() {
        assertThatCode(() -> service.sendShiftRequestReminderEmails(CalendarDateIdUtils.returnAdjustedFinalSubmissionDateTime(0).toLocalDate(), "test-idempotentKey-2"))
                .doesNotThrowAnyException();
    }
}
