package com.richardbrenkus.shiftschedulermodernized.activity;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;

import java.time.Instant;
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

    public static ActivityEvent success(
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
                Instant.now(),
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
                Instant.now(),
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
