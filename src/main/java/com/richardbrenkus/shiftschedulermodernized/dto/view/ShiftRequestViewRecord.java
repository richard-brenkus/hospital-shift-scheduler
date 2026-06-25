package com.richardbrenkus.shiftschedulermodernized.dto.view;


import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Builder
public record ShiftRequestViewRecord(
        Long shiftRequestId,
        ZonedDateTime creationDate,
        Long userId,
        String username,
        Set<Integer> enabledShiftTypes,
        Set<Integer> keepPreviousShiftTypes,
        boolean keepPreviousNoDates,
        List<ShiftPreferenceViewRecord> preferences,
        String stringDatesNo
) {
}
