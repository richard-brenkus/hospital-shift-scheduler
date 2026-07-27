package com.richardbrenkus.shiftschedulermodernized.entity;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ReminderEmailOutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.time.*;
import java.util.UUID;

@Entity
@Table(
        name = "reminder_email_outbox",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reminder_email_outbox_event_id",
                        columnNames = "event_id"
                ),
                @UniqueConstraint(
                        name = "uk_reminder_email_outbox_idempotency_key",
                        columnNames = "idempotency_key"
                ),
                @UniqueConstraint(
                        name = "uk_reminder_email_outbox_occurrence_recipient",
                        columnNames = {
                                "source_task_id",
                                "scheduled_execution_time",
                                "recipient_user_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_reminder_email_outbox_dispatch",
                        columnList = "status,next_attempt_at"
                ),
                @Index(
                        name = "idx_reminder_email_outbox_claim",
                        columnList = "status,claimed_at"
                )
        }
)
@Check(constraints = "attempt_count >= 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminderEmailOutbox {

    private static final int MAXIMUM_EMAIL_LENGTH = 320;
    private static final int MAXIMUM_DISPLAY_NAME_LENGTH = 255;
    private static final int MAXIMUM_WORKER_ID_LENGTH = 100;
    private static final int MAXIMUM_CLAIM_TOKEN_LENGTH = 36;
    private static final int MAXIMUM_FAILURE_REASON_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36, updatable = false)
    private String eventId;

    @Column(name = "idempotency_key", nullable = false, length = 150, updatable = false)
    private String idempotencyKey;

    @Column(name = "source_task_id", nullable = false, updatable = false)
    private Long sourceTaskId;

    @Column(name = "scheduled_execution_time", nullable = false, updatable = false)
    private Instant scheduledExecutionTime;

    @Column(name = "final_submission_day", nullable = false, updatable = false)
    private LocalDate finalSubmissionDay;

    @Column(name = "recipient_user_id", nullable = false, updatable = false)
    private Long recipientUserId;

    @Column(name = "recipient_email", nullable = false, length = 320, updatable = false)
    private String recipientEmail;

    @Column(name = "recipient_display_name", updatable = false)
    private String recipientDisplayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReminderEmailOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "dead_at")
    private Instant deadAt;

    @Column(name = "last_failure_reason")
    private String lastFailureReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static ReminderEmailOutbox pending(
            Long sourceTaskId,
            Instant scheduledExecutionTime,
            LocalDate finalSubmissionDay,
            Instant finalSubmissionDeadline,
            Long recipientUserId,
            String recipientEmail,
            String recipientDisplayName,
            Instant now
    ) {
        requireNonNull(sourceTaskId, "sourceTaskId");
        requireNonNull(scheduledExecutionTime, "scheduledExecutionTime");
        requireNonNull(finalSubmissionDay, "finalSubmissionDay");
        requireNonNull(recipientUserId, "recipientUserId");
        requireNonNull(now, "now");

        if (scheduledExecutionTime.isAfter(finalSubmissionDeadline)) {
            throw new IllegalArgumentException(
                    "scheduledExecutionTime must not be after finalSubmissionDay"
            );
        }

        ReminderEmailOutbox outbox = new ReminderEmailOutbox();
        String eventId = UUID.randomUUID().toString();

        outbox.eventId = eventId;
        outbox.idempotencyKey = "shift-reminder:" + sourceTaskId + ":" + recipientUserId + ":" + eventId;
        outbox.sourceTaskId = sourceTaskId;
        outbox.scheduledExecutionTime = scheduledExecutionTime;
        outbox.finalSubmissionDay = finalSubmissionDay;
        outbox.recipientUserId = recipientUserId;
        outbox.recipientEmail = normalizeRequiredEmail(recipientEmail);
        outbox.recipientDisplayName = normalizeNullableText(recipientDisplayName, MAXIMUM_DISPLAY_NAME_LENGTH);
        outbox.status = ReminderEmailOutboxStatus.PENDING;
        outbox.attemptCount = 0;
        outbox.nextAttemptAt = now;
        outbox.createdAt = now;

        return outbox;
    }

    public void claim(
            String workerId,
            String newClaimToken,
            Instant now
    ) {
        requireNonNegativeAttemptCount();
        requireNonNull(now, "now");

        if (status != ReminderEmailOutboxStatus.PENDING
                && status != ReminderEmailOutboxStatus.FAILED) {
            throw new IllegalStateException(
                    "Only PENDING or FAILED jobs can be claimed"
            );
        }

        if (nextAttemptAt == null) {
            throw new IllegalStateException(
                    "A dispatchable outbox job must have nextAttemptAt"
            );
        }

        if (now.isBefore(nextAttemptAt)) {
            throw new IllegalArgumentException(
                    "claim time must not precede nextAttemptAt"
            );
        }

        status = ReminderEmailOutboxStatus.PROCESSING;
        claimedBy = truncateRequiredIdentifier(
                workerId,
                MAXIMUM_WORKER_ID_LENGTH,
                "workerId"
        );
        claimToken = truncateRequiredIdentifier(
                newClaimToken,
                MAXIMUM_CLAIM_TOKEN_LENGTH,
                "claimToken"
        );
        claimedAt = now;
        attemptCount++;
        sentAt = null;
        deadAt = null;
        lastFailureReason = null;
    }

    public boolean isOwnedByClaim(String expectedClaimToken) {
        return status == ReminderEmailOutboxStatus.PROCESSING
                && expectedClaimToken != null
                && !expectedClaimToken.isBlank()
                && expectedClaimToken.equals(claimToken);
    }

    public void markSent(
            String expectedClaimToken,
            Instant now
    ) {
        requireNonNegativeAttemptCount();
        requireNonNull(now, "now");
        requireCurrentClaim(expectedClaimToken);
        requireNotBeforeClaim(now, "sent time");

        status = ReminderEmailOutboxStatus.SENT;
        sentAt = now;
        deadAt = null;
        clearClaim();
        lastFailureReason = null;
    }

    public void markFailed(
            String expectedClaimToken,
            String safeFailureReason,
            Instant retryAt
    ) {
        requireNonNegativeAttemptCount();
        requireNonNull(retryAt, "retryAt");
        requireCurrentClaim(expectedClaimToken);
        requireNotBeforeClaim(retryAt, "next attempt time");

        status = ReminderEmailOutboxStatus.FAILED;
        sentAt = null;
        deadAt = null;
        lastFailureReason = normalizeFailureReason(safeFailureReason);
        nextAttemptAt = retryAt;
        clearClaim();
    }

    public void markDead(
            String expectedClaimToken,
            String safeFailureReason,
            Instant now
    ) {
        requireNonNegativeAttemptCount();
        requireNonNull(now, "now");
        requireCurrentClaim(expectedClaimToken);
        requireNotBeforeClaim(now, "dead time");

        status = ReminderEmailOutboxStatus.DEAD;
        sentAt = null;
        deadAt = now;
        lastFailureReason = normalizeFailureReason(safeFailureReason);
        nextAttemptAt = now;
        clearClaim();
    }

    /**
     * Terminates a PENDING or FAILED row before a new claim is created.
     * This closes the stale-claim recovery edge case in which a row has
     * already reached the configured maximum number of attempts.
     */
    public void markDeadFromDispatchableState(
            String safeFailureReason,
            Instant now
    ) {
        requireNonNegativeAttemptCount();
        requireNonNull(now, "now");

        if (status != ReminderEmailOutboxStatus.PENDING
                && status != ReminderEmailOutboxStatus.FAILED) {
            throw new IllegalStateException(
                    "Only PENDING or FAILED jobs can be terminated without a claim"
            );
        }

        status = ReminderEmailOutboxStatus.DEAD;
        sentAt = null;
        deadAt = now;
        lastFailureReason = normalizeFailureReason(safeFailureReason);
        nextAttemptAt = now;
        clearClaim();
    }

    public void releaseStaleClaim(
            String safeFailureReason,
            Instant retryAt
    ) {
        requireNonNegativeAttemptCount();
        requireNonNull(retryAt, "retryAt");

        if (status != ReminderEmailOutboxStatus.PROCESSING) {
            return;
        }

        requireNotBeforeClaim(retryAt, "next attempt time");

        status = ReminderEmailOutboxStatus.FAILED;
        sentAt = null;
        deadAt = null;
        lastFailureReason = normalizeFailureReason(safeFailureReason);
        nextAttemptAt = retryAt;
        clearClaim();
    }

    private void requireCurrentClaim(String expectedClaimToken) {
        if (!isOwnedByClaim(expectedClaimToken)) {
            throw new IllegalStateException(
                    "The outbox job is not owned by the supplied claim token"
            );
        }
    }

    private void requireNotBeforeClaim(
            Instant value,
            String valueDescription
    ) {
        if (claimedAt != null && value.isBefore(claimedAt)) {
            throw new IllegalArgumentException(
                    valueDescription + " must not precede claim time"
            );
        }
    }

    private void requireNonNegativeAttemptCount() {
        if (attemptCount < 0) {
            throw new IllegalStateException(
                    "attemptCount must not be negative"
            );
        }
    }

    private void clearClaim() {
        claimedAt = null;
        claimedBy = null;
        claimToken = null;
    }

    private static String normalizeRequiredEmail(String recipientEmail) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "recipientEmail must not be blank"
            );
        }

        String normalizedEmail = recipientEmail.trim();

        if (normalizedEmail.length() > MAXIMUM_EMAIL_LENGTH) {
            throw new IllegalArgumentException(
                    "recipientEmail must not exceed 320 characters"
            );
        }

        return normalizedEmail;
    }

    private static String truncateRequiredIdentifier(
            String value,
            int maximumLength,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return truncateTrimmed(value, maximumLength);
    }

    private static String normalizeFailureReason(String value) {
        if (value == null || value.isBlank()) {
            return "Reminder email delivery failed";
        }

        return truncateTrimmed(value, MAXIMUM_FAILURE_REASON_LENGTH);
    }

    private static String normalizeNullableText(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return truncateTrimmed(value, maximumLength);
    }

    private static String truncateTrimmed(String value, int maximumLength) {
        String trimmed = value.trim();

        return trimmed.length() <= maximumLength
                ? trimmed
                : trimmed.substring(0, maximumLength);
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null"
            );
        }
    }
}
