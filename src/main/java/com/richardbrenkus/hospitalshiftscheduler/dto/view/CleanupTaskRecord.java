package com.richardbrenkus.hospitalshiftscheduler.dto.view;

import java.time.ZonedDateTime;

public record CleanupTaskRecord(
        boolean cleanupIsActive,
        ZonedDateTime cleanupDateTime
) {
}
