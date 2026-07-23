package com.richardbrenkus.shiftschedulermodernized.entity;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ReminderEmailOutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminderEmailOutbox {

    private static final int MAXIMUM_EMAIL_LENGTH = 320;

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
    private LocalDateTime scheduledExecutionTime;

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
    private LocalDateTime nextAttemptAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "last_failure_reason")
    private String lastFailureReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static ReminderEmailOutbox pending(
            Long sourceTaskId,
            LocalDateTime scheduledExecutionTime,
            LocalDate finalSubmissionDay,
            Long recipientUserId,
            String recipientEmail,
            String recipientDisplayName,
            LocalDateTime now
    ) {
        requireNonNull(sourceTaskId, "sourceTaskId");
        requireNonNull(scheduledExecutionTime, "scheduledExecutionTime");
        requireNonNull(finalSubmissionDay, "finalSubmissionDay");
        requireNonNull(recipientUserId, "recipientUserId");
        requireNonNull(now, "now");

        String normalizedEmail = normalizeRequiredEmail(recipientEmail);

        ReminderEmailOutbox outbox = new ReminderEmailOutbox();
        String eventId = UUID.randomUUID().toString();

        outbox.eventId = eventId;
        outbox.idempotencyKey =
                "shift-reminder:" + sourceTaskId + ":" + recipientUserId + ":" + eventId;
        outbox.sourceTaskId = sourceTaskId;
        outbox.scheduledExecutionTime = scheduledExecutionTime;
        outbox.finalSubmissionDay = finalSubmissionDay;
        outbox.recipientUserId = recipientUserId;
        outbox.recipientEmail = normalizedEmail;
        outbox.recipientDisplayName = normalizeNullable(recipientDisplayName, 255);
        outbox.status = ReminderEmailOutboxStatus.PENDING;
        outbox.attemptCount = 0;
        outbox.nextAttemptAt = now;
        outbox.createdAt = now;

        return outbox;
    }

    public void claim(
            String workerId,
            String claimToken,
            LocalDateTime now
    ) {
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId must not be blank");
        }
        if (claimToken == null || claimToken.isBlank()) {
            throw new IllegalArgumentException("claimToken must not be blank");
        }
        requireNonNull(now, "now");

        if (status != ReminderEmailOutboxStatus.PENDING
                && status != ReminderEmailOutboxStatus.FAILED) {
            throw new IllegalStateException(
                    "Only PENDING or FAILED jobs can be claimed"
            );
        }

        status = ReminderEmailOutboxStatus.PROCESSING;
        claimedBy = truncate(workerId, 100);
        this.claimToken = truncate(claimToken, 36);
        claimedAt = now;
        attemptCount++;
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
            LocalDateTime now
    ) {
        requireNonNull(now, "now");
        requireCurrentClaim(expectedClaimToken);

        status = ReminderEmailOutboxStatus.SENT;
        sentAt = now;
        clearClaim();
        lastFailureReason = null;
    }

    public void markFailed(
            String expectedClaimToken,
            String safeFailureReason,
            LocalDateTime nextAttemptAt
    ) {
        requireNonNull(nextAttemptAt, "nextAttemptAt");
        requireCurrentClaim(expectedClaimToken);

        status = ReminderEmailOutboxStatus.FAILED;
        clearClaim();
        sentAt = null;
        lastFailureReason = truncate(safeFailureReason, 255);
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markDead(
            String expectedClaimToken,
            LocalDateTime now
    ) {
        requireNonNull(now, "now");
        requireCurrentClaim(expectedClaimToken);
        status = ReminderEmailOutboxStatus.DEAD;
        clearClaim();
    }



    public void releaseStaleClaim(
            String safeFailureReason,
            LocalDateTime nextAttemptAt
    ) {
        requireNonNull(nextAttemptAt, "nextAttemptAt");

        if (status != ReminderEmailOutboxStatus.PROCESSING) {
            return;
        }

        status = ReminderEmailOutboxStatus.FAILED;
        clearClaim();
        sentAt = null;
        lastFailureReason = truncate(safeFailureReason, 255);
        this.nextAttemptAt = nextAttemptAt;
    }

    private void requireCurrentClaim(String expectedClaimToken) {
        if (!isOwnedByClaim(expectedClaimToken)) {
            throw new IllegalStateException(
                    "The outbox job is not owned by the supplied claim token"
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

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null"
            );
        }
    }

    private static String normalizeNullable(
            String value,
            int maximumLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return truncate(value, maximumLength);
    }

    private static String truncate(
            String value,
            int maximumLength
    ) {
        if (value == null || value.isBlank()) {
            return "Unknown failure";
        }

        String trimmed = value.trim();

        return trimmed.length() <= maximumLength
                ? trimmed
                : trimmed.substring(0, maximumLength);
    }
}