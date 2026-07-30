package com.richardbrenkus.hospitalshiftscheduler.algorithm.record;

import java.time.YearMonth;
import java.util.List;

import java.time.LocalDate;
import java.util.Objects;

public record CalculationInput(
        YearMonth month,
        List<UserCalculationData> users,
        List<Integer> shiftTypes,
        List<Integer> calculationOrder,
        List<Integer> priorities,
        List<LocalDate> holidays,
        CalculationProfile profile
) {
    public CalculationInput {
        Objects.requireNonNull(month, "month must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        users = users == null ? List.of() : List.copyOf(users);
        shiftTypes = shiftTypes == null ? List.of() : List.copyOf(shiftTypes);
        calculationOrder = calculationOrder == null ? List.of() : List.copyOf(calculationOrder);
        priorities = priorities == null ? List.of() : List.copyOf(priorities);
        holidays = holidays == null ? List.of() : List.copyOf(holidays);
    }
}

