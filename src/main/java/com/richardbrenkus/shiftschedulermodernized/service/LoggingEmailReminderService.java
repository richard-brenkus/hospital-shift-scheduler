package com.richardbrenkus.shiftschedulermodernized.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Profile("!prod")
@Slf4j
public class LoggingEmailReminderService
        implements EmailReminderService {

    private static final int MAXIMUM_EMAIL_LENGTH = 320;

    @Override
    public void sendShiftRequestReminderEmail(
            String recipientEmail,
            String recipientDisplayName,
            LocalDate finalSubmissionDate,
            String idempotencyKey
    ) {
        validateArguments(
                recipientEmail,
                finalSubmissionDate,
                idempotencyKey
        );

        log.info(
                "Reminder email would be sent to {}. "
                        + "Recipient name = {}, deadline = {}, "
                        + "idempotency key = {}",
                recipientEmail.trim(),
                normalizeDisplayName(recipientDisplayName),
                finalSubmissionDate,
                idempotencyKey.trim()
        );
    }

    private void validateArguments(
            String recipientEmail,
            LocalDate finalSubmissionDate,
            String idempotencyKey
    ) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "recipientEmail must not be blank"
            );
        }

        if (recipientEmail.trim().length() > MAXIMUM_EMAIL_LENGTH) {
            throw new IllegalArgumentException(
                    "recipientEmail must not exceed 320 characters"
            );
        }

        if (finalSubmissionDate == null) {
            throw new IllegalArgumentException(
                    "finalSubmissionDate must not be null"
            );
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "idempotencyKey must not be blank"
            );
        }
    }

    private String normalizeDisplayName(
            String recipientDisplayName
    ) {
        if (recipientDisplayName == null
                || recipientDisplayName.isBlank()) {
            return null;
        }

        return recipientDisplayName.trim();
    }
}