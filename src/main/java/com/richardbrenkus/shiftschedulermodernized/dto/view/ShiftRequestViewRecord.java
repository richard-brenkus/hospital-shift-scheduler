package com.richardbrenkus.shiftschedulermodernized.dto.view;


import lombok.Builder;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Builder
public record ShiftRequestViewRecord(
        Long shiftRequestId,
        ZonedDateTime creationDate,
        Long userId,
        String username,
        String displayName,
        List<LocalDate> datesNo,
        Set<Integer> enabledShiftTypes,
        Set<Integer> keepPreviousShiftTypes,
        boolean keepPreviousNoDates,
        List<ShiftPreferenceViewRecord> preferences
) {
}
