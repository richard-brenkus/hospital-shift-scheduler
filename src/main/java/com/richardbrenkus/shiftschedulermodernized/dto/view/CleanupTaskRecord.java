package com.richardbrenkus.shiftschedulermodernized.dto.view;

import java.time.Instant;

public record CleanupTaskRecord(
        boolean cleanupIsActive,
        Instant cleanupDateTime
) {
}
