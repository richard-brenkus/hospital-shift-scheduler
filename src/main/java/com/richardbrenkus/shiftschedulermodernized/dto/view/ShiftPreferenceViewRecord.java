package com.richardbrenkus.shiftschedulermodernized.dto.view;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record ShiftPreferenceViewRecord(
        int shiftType,
        List<LocalDate> datesYes,
        boolean anyDateSelected,
        boolean noShiftRequested,
        int priority,
        int weekdayCount,
        int weekendCount
) {
}
