package com.richardbrenkus.shiftschedulermodernized.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class SmtpEmailReminderService
        implements EmailReminderService {

    private static final int MAXIMUM_EMAIL_LENGTH = 320;

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.mail.message-id-domain:localhost}")
    private String messageIdDomain;

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
        validateSenderConfiguration();

        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress.trim());
            helper.setTo(recipientEmail.trim());
            helper.setSubject("Shift request reminder");
            helper.setText(
                    buildMessageText(
                            recipientDisplayName,
                            finalSubmissionDate
                    ),
                    false
            );

            message.setHeader(
                    "Message-ID",
                    buildMessageId(idempotencyKey)
            );

            message.setHeader(
                    "X-Idempotency-Key",
                    idempotencyKey.trim()
            );

            mailSender.send(message);

        } catch (MessagingException exception) {
            throw new MailPreparationException(
                    "Could not prepare reminder email",
                    exception
            );
        }
    }

    private String buildMessageText(
            String recipientDisplayName,
            LocalDate finalSubmissionDate
    ) {
        String name =
                recipientDisplayName == null
                        || recipientDisplayName.isBlank()
                        ? "user"
                        : recipientDisplayName.trim();

        return """
                Hello %s,

                this is a reminder to submit your shift request.

                Please submit your request by day %d of the month.

                Thank you.
                """.formatted(
                name,
                finalSubmissionDate.getDayOfMonth()
        );
    }

    private String buildMessageId(String idempotencyKey) {
        return "<"
                + sanitizeMessageIdPart(idempotencyKey)
                + "@"
                + sanitizeMessageIdPart(messageIdDomain)
                + ">";
    }

    private String sanitizeMessageIdPart(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value.trim()
                .replaceAll("[^A-Za-z0-9._-]", "-");
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

    private void validateSenderConfiguration() {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException(
                    "spring.mail.username must be configured"
            );
        }

        if (fromAddress.trim().length() > MAXIMUM_EMAIL_LENGTH) {
            throw new IllegalStateException(
                    "spring.mail.username must not exceed 320 characters"
            );
        }

        if (messageIdDomain == null || messageIdDomain.isBlank()) {
            throw new IllegalStateException(
                    "app.mail.message-id-domain must not be blank"
            );
        }
    }
}