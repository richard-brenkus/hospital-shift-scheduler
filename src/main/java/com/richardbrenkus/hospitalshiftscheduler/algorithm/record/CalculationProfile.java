package com.richardbrenkus.hospitalshiftscheduler.algorithm.record;

import java.util.List;

public record CalculationProfile(int shiftCountCap, int gapBetweenShifts, boolean sortByDatesAmount, List<Integer> forceFillShiftTypes) {

    public CalculationProfile {
        if (shiftCountCap < 0) {
            throw new IllegalArgumentException("shiftCountCap must not be negative");
        }
        if (gapBetweenShifts < 0) {
            throw new IllegalArgumentException("gapBetweenShifts must not be negative");
        }
        forceFillShiftTypes = forceFillShiftTypes == null ? List.of() : List.copyOf(forceFillShiftTypes);
    }
}

