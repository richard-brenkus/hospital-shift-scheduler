package com.richardbrenkus.hospitalshiftscheduler.service;

import java.time.LocalDate;

/**
 * Sends a single shift-request reminder email.
 *
 * <p>Each invocation corresponds to exactly one
 * {@link com.richardbrenkus.hospitalshiftscheduler.entity.ReminderEmailOutbox}
 * row. The caller is responsible for iterating over pending outbox jobs.</p>
 */
public interface EmailReminderService {

    /**
     * Sends one reminder email to one recipient.
     *
     * @param recipientEmail recipient email address
     * @param recipientDisplayName recipient display name (may be null)
     * @param finalSubmissionDate last day for submitting the shift request
     * @param idempotencyKey stable identifier associated with the outbox row
     */
    void sendShiftRequestReminderEmail(String recipientEmail, String recipientDisplayName, LocalDate finalSubmissionDate, String idempotencyKey);
}
