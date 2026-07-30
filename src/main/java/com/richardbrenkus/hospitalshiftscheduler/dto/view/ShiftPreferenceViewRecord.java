package com.richardbrenkus.hospitalshiftscheduler.dto.view;

import lombok.Builder;

@Builder
public record ShiftPreferenceViewRecord(
        int shiftType,
        boolean anyDateSelected,
        boolean noShiftRequested,
        int priority,
        int weekdayCount,
        int weekendCount,
        String stringDatesYes
) {
}
