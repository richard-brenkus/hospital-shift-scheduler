package com.richardbrenkus.shiftschedulermodernized.dto.view;

import java.time.LocalDate;
import java.util.List;

public record ShiftPreferenceViewRecord(
        int shiftType,
        List<LocalDate> datesYes,
        boolean anyDateSelectionAllowed,
        boolean shiftUnwanted
) {
}
