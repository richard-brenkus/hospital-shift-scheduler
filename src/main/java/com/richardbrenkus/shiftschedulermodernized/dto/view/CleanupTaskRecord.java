package com.richardbrenkus.shiftschedulermodernized.dto.view;

import java.time.LocalDateTime;

public record CleanupTaskRecord(
        boolean cleanupIsActive,
        LocalDateTime cleanupDateTime
) {
}
