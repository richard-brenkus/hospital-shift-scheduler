package com.richardbrenkus.shiftschedulermodernized.activity.kafka;

import java.time.Instant;
import java.util.UUID;

/**
 * Stable Kafka payload representing an activity event.
 * Enum values are deliberately converted to strings before publishing. This
 * keeps the transport DTO independent from the application's enum classes and
 * makes the serialized contract explicit.
 */
public record ActivityKafkaMessage(
        UUID eventId,
        Instant occurredAt,
        String activityType,
        String actorUsername,
        String actorRole,
        String targetType,
        String targetId,
        String description,
        boolean successful,
        String failureReason,
        String requestMethod,
        String requestPath,
        String clientIp
) {
}
