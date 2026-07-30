package com.richardbrenkus.hospitalshiftscheduler.dto.view;

import lombok.Builder;

import java.util.List;
import java.util.Set;

@Builder
public record ShiftRequestViewRecord(
        Set<Integer> enabledShiftTypes,
        List<ShiftPreferenceViewRecord> preferences,
        String stringDatesNo
) {
}
