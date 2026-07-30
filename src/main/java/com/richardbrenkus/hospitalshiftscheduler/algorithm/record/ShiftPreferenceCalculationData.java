package com.richardbrenkus.hospitalshiftscheduler.algorithm.record;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Set;

public record ShiftPreferenceCalculationData(int shiftType,
                                             int priority,
                                             int weekdayCount,
                                             int weekendCount,
                                             boolean noShiftRequested,
                                             boolean anyDateSelected,
                                             Set<LocalDate> requestedDates) {

    public ShiftPreferenceCalculationData {
        if (shiftType <= 0) {
            throw new IllegalArgumentException("shiftType must be positive");
        }

        if (priority <= 0) {
            throw new IllegalArgumentException("priority must be positive");
        }

        if (weekdayCount < 0) {
            throw new IllegalArgumentException("weekdayCount must not be negative");
        }

        if (weekendCount < 0) {
            throw new IllegalArgumentException("weekendCount must not be negative");
        }

        requestedDates = requestedDates == null ? Set.of() : Set.copyOf(requestedDates);
    }

    public boolean appliesToMonth(YearMonth month) {
        Objects.requireNonNull(month, "month must not be null");

        return anyDateSelected || requestedDates.stream().anyMatch(date -> date != null && YearMonth.from(date).equals(month));
    }

    public boolean acceptsDate(LocalDate date) {
        if (date == null || noShiftRequested) {
            return false;
        }

        return anyDateSelected || requestedDates.contains(date);
    }

    public int requestedDatesInMonth(YearMonth month) {
        Objects.requireNonNull(month, "month must not be null");

        return Math.toIntExact(requestedDates.stream().filter(date -> date != null && YearMonth.from(date).equals(month)).count());
    }
}
