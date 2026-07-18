package com.richardbrenkus.shiftschedulermodernized.algorithm.record;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of all user-related data required by the schedule
 * calculation.
 * No JPA entity or Hibernate-managed collection should be stored here.
 */
public record UserCalculationData(
        Long userId,
        String name,
        String username,
        String title,
        Set<Integer> allowedShiftTypes,
        Set<LocalDate> unavailableDates,
        Map<Integer, ShiftPreferenceCalculationData> preferencesByShiftType,
        Set<LocalDate> previousMonthAssignedDates
) {

    public UserCalculationData {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(username, "username must not be null");

        allowedShiftTypes = allowedShiftTypes == null
                ? Set.of()
                : Set.copyOf(allowedShiftTypes);

        unavailableDates = unavailableDates == null
                ? Set.of()
                : Set.copyOf(unavailableDates);

        preferencesByShiftType = preferencesByShiftType == null
                ? Map.of()
                : Map.copyOf(preferencesByShiftType);

        previousMonthAssignedDates =
                previousMonthAssignedDates == null
                        ? Set.of()
                        : Set.copyOf(previousMonthAssignedDates);
    }

    public boolean canWorkShiftType(int shiftType) {
        return allowedShiftTypes.contains(shiftType);
    }

    public boolean isUnavailableOn(LocalDate date) {
        return date != null && unavailableDates.contains(date);
    }

    public Optional<ShiftPreferenceCalculationData> preferenceFor(int shiftType) {
        return Optional.ofNullable(preferencesByShiftType.get(shiftType));
    }

    public static Map<Integer, ShiftPreferenceCalculationData>
    indexPreferences(List<ShiftPreferenceCalculationData> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return Map.of();
        }

        return preferences.stream().collect(Collectors.toUnmodifiableMap(ShiftPreferenceCalculationData::shiftType, Function.identity()));
    }
}
