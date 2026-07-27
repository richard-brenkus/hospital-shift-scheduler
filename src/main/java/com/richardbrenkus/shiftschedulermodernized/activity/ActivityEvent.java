package com.richardbrenkus.shiftschedulermodernized.activity;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ActivityEvent(
        UUID eventId,
        Instant occurredAt,
        String actorUsername,
        Role actorRole,
        ActivityType activityType,
        String targetType,
        String targetId,
        String description,
        boolean successful,
        String failureReason,
        RequestMetadata requestMetadata
) {

    public ActivityEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(actorUsername, "actorUsername must not be null");
        Objects.requireNonNull(actorRole, "actorRole must not be null");
        Objects.requireNonNull(activityType, "activityType must not be null");
        Objects.requireNonNull(requestMetadata, "requestMetadata must not be null");
    }

    public static ActivityEvent success(
            Instant occurredAt,
            ActivityType activityType,
            String actorUsername,
            Role actorRole,
            String targetType,
            String targetId,
            String description,
            RequestMetadata requestMetadata
    ) {
        return new ActivityEvent(
                UUID.randomUUID(),
                occurredAt,
                actorUsername,
                actorRole,
                activityType,
                targetType,
                targetId,
                description,
                true,
                null,
                requestMetadata
        );
    }

    public static ActivityEvent failure(
            Instant occurredAt,
            ActivityType activityType,
            String actorUsername,
            Role actorRole,
            String targetType,
            String targetId,
            String description,
            String failureReason,
            RequestMetadata requestMetadata
    ) {
        return new ActivityEvent(
                UUID.randomUUID(),
                occurredAt,
                actorUsername,
                actorRole,
                activityType,
                targetType,
                targetId,
                description,
                false,
                failureReason,
                requestMetadata
        );
    }
}