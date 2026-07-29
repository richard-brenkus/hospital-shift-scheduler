package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.util.CalendarDateIdUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoggingEmailReminderServiceTest {

    private final LoggingEmailReminderService service = new LoggingEmailReminderService();

    @Test
    void shouldNotThrow_whenSendingReminder() {
        assertThatCode(() -> service.sendShiftRequestReminderEmail("recipient@example.test", "Recipient Name", CalendarDateIdUtils.returnAdjustedFinalSubmissionDateTime(20).toLocalDate(), "test-idempotentKey-1")).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectBlankRecipientEmail() {
        assertThatThrownBy(() -> service.sendShiftRequestReminderEmail("", "Recipient Name", CalendarDateIdUtils.returnAdjustedFinalSubmissionDateTime(20).toLocalDate(), "test-idempotentKey-2")).isInstanceOf(IllegalArgumentException.class);
    }
}
