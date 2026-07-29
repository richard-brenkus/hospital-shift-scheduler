package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.*;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ShiftPreferenceCalculationData;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.UserCalculationData;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredScheduleDayRepository;
import com.richardbrenkus.shiftschedulermodernized.util.CalendarDateIdUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ScheduleRuleService {

    private final StoredScheduleDayRepository storedScheduleDayRepository;

    boolean isWithinRequestedWeekdayLimit(UserCalculationData userCalculationData, int shiftType, CalculationCounters counters) {

        ShiftPreferenceCalculationData preference = getPreference(userCalculationData, shiftType);

        if (preference == null) {
            return false;
        }

        int requestedWeekdayCount = preference.weekdayCount();
        int currentWeekdayCount = getShiftCounter(userCalculationData, shiftType, counters);

        return requestedWeekdayCount != 0
                && currentWeekdayCount < requestedWeekdayCount;
    }

    boolean isWithinRequestedWeekendLimit(UserCalculationData userCalculationData, int shiftType, CalculationCounters counters) {

        ShiftPreferenceCalculationData preference = getPreference(userCalculationData, shiftType);

        if (preference == null) {
            return false;
        }

        int requestedWeekendCount = preference.weekendCount();
        int currentWeekendCount = getWeekendCounter(userCalculationData, shiftType, counters);

        return requestedWeekendCount != 0
                && currentWeekendCount < requestedWeekendCount;
    }

    private ShiftPreferenceCalculationData getPreference(UserCalculationData userCalculationData, int shiftType) {
        if (!userCalculationData.hasShiftRequest()) {
            return null;
        }

        return userCalculationData.preferenceFor(shiftType).orElse(null);
    }

    private int getShiftCounter(UserCalculationData user, int shiftType, CalculationCounters counters) {
        return counters.getWeekdayCounters()
                .getOrDefault(user.userId(), Map.of())
                .getOrDefault(shiftType, 0);
    }

    private int getWeekendCounter(UserCalculationData user, int shiftType, CalculationCounters counters) {
        return counters.getWeekendCounters()
                .getOrDefault(user.userId(), Map.of())
                .getOrDefault(shiftType, 0);
    }

    boolean respectsMinimalGap(LocalDate date, int minimalGap, UserCalculationData userCalculationData, ScheduleMonth scheduleMonth, int currentShiftType) {
        if (date == null || userCalculationData == null || userCalculationData.userId() == null || scheduleMonth == null) {
            return true;
        }

        LocalDate startDate = date.minusDays(minimalGap);
        LocalDate endDate = date.plusDays(minimalGap);

        for (ScheduleDay day : scheduleMonth.getDays()) {

            if (day == null || day.getDate() == null || day.getAssignments() == null) {
                continue;
            }

            LocalDate checkedDate = day.getDate();

            if (checkedDate.isBefore(startDate) || checkedDate.isAfter(endDate)) {
                continue;
            }

            for (ShiftAssignment assignment : day.getAssignments()) {

                if (assignment == null || assignment.getUserCalculationData() == null) {
                    continue;
                }

                boolean sameSlot =
                        checkedDate.equals(date)
                                && assignment.getShiftType() == currentShiftType;

                if (sameSlot) {
                    continue;
                }

                UserCalculationData assignedUserCalculationData = assignment.getUserCalculationData();

                if (Objects.equals(assignedUserCalculationData.userId(), userCalculationData.userId())) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean respectsPreviousMonthGap(Map<Integer, StoredScheduleDay> previousMonthStoredScheduleDays, Integer minimalGap, LocalDate date, UserCalculationData userCalculationData) {
        if (previousMonthStoredScheduleDays == null
                || minimalGap == null
                || date == null
                || userCalculationData == null
                || userCalculationData.username() == null) {
            return true;
        }

        if (date.getDayOfMonth() - minimalGap > 0) {
            return true;
        }

        for (int backwardIndex = 0; backwardIndex > -minimalGap; backwardIndex--) {
            StoredScheduleDay previousMonthDay =
                    previousMonthStoredScheduleDays.get(backwardIndex);

            if (previousMonthDay == null) {
                continue;
            }

            boolean userWorkedPreviousMonthDay = previousMonthDay.getAssignmentsByShiftType()
                            .values()
                            .stream()
                            .anyMatch(snapshot ->
                                    snapshot != null
                                            && userCalculationData.username().equals(snapshot.getUsername())
                            );

            if (userWorkedPreviousMonthDay) {
                return false;
            }
        }

        return true;
    }


    boolean isNotRejectedByUser(LocalDate date, Set<LocalDate> datesNo) {
        if (datesNo == null || date == null) {
            return true;
        }

        return !datesNo.contains(date);
    }

    boolean isValidWithinTotalShiftLimit(Integer shiftCountCap, UserCalculationData user, CalculationCounters counters) {
        if (user == null || !user.hasShiftRequest()) {
            return false;
        }

        if (shiftCountCap == null) {
            return true;
        }

        return counters.getTotalCount(user.userId()) <= shiftCountCap;
    }

    boolean isValidWithinRequestedWeekendLimit(UserCalculationData userCalculationData, int shiftType, CalculationCounters counters) {
        if (userCalculationData == null || !userCalculationData.hasShiftRequest()) {
            return false;
        }

        ShiftPreferenceCalculationData preference = getPreference(userCalculationData, shiftType);

        int requestedWeekendCount = preference == null ? 0 : preference.weekendCount();

        int assignedWeekendCount = counters.getWeekendCount(userCalculationData.userId(), shiftType);

        return assignedWeekendCount <= requestedWeekendCount;
    }

    boolean isValidWithinRequestedWeekdayLimit(UserCalculationData userCalculationData, int shiftType, CalculationCounters counters) {
        if (userCalculationData == null || !userCalculationData.hasShiftRequest()) {
            return false;
        }

        ShiftPreferenceCalculationData preference = getPreference(userCalculationData, shiftType);

        int requestedWeekdayCount = preference == null ? 0 : preference.weekdayCount();

        int assignedWeekdayCount = counters.getWeekdayCount(userCalculationData.userId(), shiftType);

        return assignedWeekdayCount <= requestedWeekdayCount;
    }

    public Map<Integer, StoredScheduleDay> loadPreviousStoredScheduleDays(LocalDate adminDate, int minimalGap) {
        List<LocalDate> previousMonthDates = createPreviousMonthDatesToCheck(adminDate, minimalGap);

        Map<Integer, StoredScheduleDay> previousMonthStoredScheduleDays= new HashMap<>();

        int backwardIndex = 0;

        for (LocalDate previousMonthDate : previousMonthDates) {
            Long dateId = CalendarDateIdUtils.toDateId(previousMonthDate);

            int finalBackwardIndex = backwardIndex;
            storedScheduleDayRepository.findById(dateId)
                    .ifPresent(storedDay ->
                            previousMonthStoredScheduleDays.put(finalBackwardIndex, storedDay)
                    );

            backwardIndex--;
        }

        return previousMonthStoredScheduleDays;
    }

    private static List<LocalDate> createPreviousMonthDatesToCheck(LocalDate adminDate, int minimalGap) {
        LocalDate firstDayOfAdminMonth = adminDate.withDayOfMonth(1);

        return IntStream.rangeClosed(1, minimalGap)
                .mapToObj(firstDayOfAdminMonth::minusDays)
                .toList();
    }

    // Overloaded methods for multithreading:
    boolean isWithinTotalShiftLimit(Integer shiftCountCap,
            UserCalculationData user,
            CalculationCounters counters
    ) {
        if (shiftCountCap == null) return true;
        return counters.getTotalCount(user.userId()) < shiftCountCap;
    }

    boolean respectsMinimalGap(
            LocalDate date,
            int minimalGap,
            UserCalculationData user,
            CalculatedScheduleMonth scheduleMonth,
            int currentShiftType
    ) {
        if (date == null
                || user == null
                || user.userId() == null
                || scheduleMonth == null
                || minimalGap <= 0) {
            return true;
        }

        LocalDate startDate = date.minusDays(minimalGap);
        LocalDate endDate = date.plusDays(minimalGap);

        for (CalculatedScheduleDay day : scheduleMonth.getDays()) {
            if (day == null || day.getDate() == null || day.getAssignments() == null) continue;
            LocalDate checkedDate = day.getDate();
            if (checkedDate.isBefore(startDate) || checkedDate.isAfter(endDate)) continue;

            boolean assignedInsideGap = day.getAssignments().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(assignment -> {
                        boolean sameSlot = checkedDate.equals(date)
                                && assignment.shiftType() == currentShiftType;
                        return !sameSlot && Objects.equals(assignment.userId(), user.userId());
                    });

            if (assignedInsideGap) return false;
        }
        return true;
    }

    boolean respectsPreviousMonthGap(
            int minimalGap,
            LocalDate candidateDate,
            UserCalculationData user,
            YearMonth calculationMonth
    ) {
        if (minimalGap <= 0
                || candidateDate == null
                || user == null
                || calculationMonth == null) {
            return true;
        }

        LocalDate firstDay = calculationMonth.atDay(1);

        if (!candidateDate.isBefore(firstDay.plusDays(minimalGap))) {
            return true;
        }

        LocalDate earliestAllowed = candidateDate.minusDays(minimalGap);

        return user.previousMonthAssignedDates().stream()
                .noneMatch(previousDate ->
                        !previousDate.isBefore(earliestAllowed)
                                && previousDate.isBefore(firstDay)
                );
    }



}
