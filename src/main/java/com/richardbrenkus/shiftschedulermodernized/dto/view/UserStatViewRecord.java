package com.richardbrenkus.shiftschedulermodernized.dto.view;

import com.richardbrenkus.shiftschedulermodernized.entity.User;
import lombok.Builder;

import java.time.YearMonth;
import java.util.Set;

@Builder
public record UserStatViewRecord(
        User user,
        String name,
        int shiftType,

        int requestedWeekdays,
        int requestedWeekends,

        int calculatedWeekdays,
        int calculatedWeekends,

        int remainingWeekdays,
        int remainingWeekends,

        boolean anyDateSelected,

        Set<Integer> requestedDateDays,

        int assignedWeekdays,
        int assignedWeekends,
        int assignedTotal,
        int assignedTotalAllShiftTypes,

        Set<Integer> assignedDateDays,

        YearMonth month,

        String allowedShiftTypesAsCommaSeparatedString
) {

    public UserStatViewRecord withAssignedTotalAllShiftTypes(int assignedTotalAllShiftTypes) {
        return UserStatViewRecord.builder()
                .user(user)
                .name(name)
                .shiftType(shiftType)
                .requestedWeekdays(requestedWeekdays)
                .requestedWeekends(requestedWeekends)
                .calculatedWeekdays(calculatedWeekdays)
                .calculatedWeekends(calculatedWeekends)
                .remainingWeekdays(remainingWeekdays)
                .remainingWeekends(remainingWeekends)
                .anyDateSelected(anyDateSelected)
                .requestedDateDays(requestedDateDays)
                .assignedWeekdays(assignedWeekdays)
                .assignedWeekends(assignedWeekends)
                .assignedTotal(assignedTotal)
                .assignedTotalAllShiftTypes(assignedTotalAllShiftTypes)
                .assignedDateDays(assignedDateDays)
                .month(month)
                .build();
    }
}
