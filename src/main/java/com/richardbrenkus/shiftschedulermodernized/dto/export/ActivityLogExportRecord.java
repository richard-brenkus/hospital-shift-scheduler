package com.richardbrenkus.shiftschedulermodernized.dto.export;

import java.time.Instant;
import java.util.UUID;

public record ActivityLogExportRecord(
        Long id,
        Instant occurredAt,
        UUID eventId,
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