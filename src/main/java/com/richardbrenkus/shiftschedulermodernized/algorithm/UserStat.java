package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.entity.User;
import lombok.Builder;

import java.time.YearMonth;
import java.util.Set;

@Builder
public record UserStat(

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

        YearMonth month
) {

    public String monthName() {
        return month == null ? "" : month.getMonth().name();
    }

    public int monthInt() {
        return month == null ? 0 : month.getMonthValue();
    }

    public int year() {
        return month == null ? 0 : month.getYear();
    }
}
