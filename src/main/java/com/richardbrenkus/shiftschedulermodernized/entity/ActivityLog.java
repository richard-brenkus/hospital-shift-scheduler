package com.richardbrenkus.shiftschedulermodernized.entity;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityEvent;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "activity_log",
        indexes = {
                @Index(name = "idx_activity_log_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_activity_log_actor_username", columnList = "actor_username"),
                @Index(name = "idx_activity_log_activity_type", columnList = "activity_type")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_activity_log_event_id", columnNames = "event_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_username", nullable = false, updatable = false)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, updatable = false, length = 20)
    private Role actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, updatable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "target_type", updatable = false)
    private String targetType;

    @Column(name = "target_id", updatable = false)
    private String targetId;

    @Column(length = 1000, updatable = false)
    private String description;

    @Column(nullable = false, updatable = false)
    private boolean successful;

    @Column(name = "failure_reason", length = 1000, updatable = false)
    private String failureReason;

    @Column(name = "request_method", updatable = false)
    private String requestMethod;

    @Column(name = "request_path", updatable = false)
    private String requestPath;

    @Column(name = "client_ip", updatable = false)
    private String clientIp;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    private ActivityLog(ActivityEvent event) {
        this.occurredAt = event.occurredAt();
        this.actorUsername = event.actorUsername();
        this.actorRole = event.actorRole();
        this.activityType = event.activityType();
        this.targetType = event.targetType();
        this.targetId = event.targetId();
        this.description = event.description();
        this.successful = event.successful();
        this.failureReason = event.failureReason();
        this.requestMethod = event.requestMetadata().requestMethod();
        this.requestPath = event.requestMetadata().requestPath();
        this.clientIp = event.requestMetadata().clientIp();
        this.eventId = event.eventId();
    }

    public static ActivityLog from(ActivityEvent event) {
        return new ActivityLog(event);
    }
}