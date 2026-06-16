package com.richardbrenkus.shiftschedulermodernized.dto.view;

import java.time.LocalDate;
import java.util.List;

public record ShiftPreferenceViewRecord(
        int shiftType,
        int priority,
        List<LocalDate> datesYes,
        int weekendCount,
        int shiftCount,
        boolean anyDate,
        boolean noShift
) {
}
