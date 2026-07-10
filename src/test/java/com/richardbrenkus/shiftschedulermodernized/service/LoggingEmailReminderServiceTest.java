package com.richardbrenkus.shiftschedulermodernized.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggingEmailReminderServiceTest {

    private final LoggingEmailReminderService service = new LoggingEmailReminderService();

    @Test
    void shouldNotThrow_whenSendingReminder() {
        assertThatCode(() -> service.sendShiftRequestReminderEmails(20))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrow_whenSubmissionDayIsZero() {
        assertThatCode(() -> service.sendShiftRequestReminderEmails(0))
                .doesNotThrowAnyException();
    }
}
