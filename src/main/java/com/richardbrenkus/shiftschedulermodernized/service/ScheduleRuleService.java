package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.*;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredCalendarDay;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredCalendarDayRepository;
import com.richardbrenkus.shiftschedulermodernized.util.CalendarDateIdUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ScheduleRuleService {

    private final StoredCalendarDayRepository storedCalendarDayRepository;

    boolean isWithinTotalShiftLimit(Integer shiftCountCap, User user, CalculationCounters counters) {

        if (shiftCountCap == null) {
            return true;
        }

        int shiftCountTotal = getTotalShiftCount(user, counters);

        if (shiftCountTotal == 0) {
            return true;
        }

        return shiftCountTotal < shiftCountCap;
    }

    private int getTotalShiftCount(User user, CalculationCounters counters) {
        Map<Integer, Integer> userShiftCounters =
                counters.getWeekdayCounters().getOrDefault(user.getId(), Map.of());

        Map<Integer, Integer> userWeekendCounters =
                counters.getWeekendCounters().getOrDefault(user.getId(), Map.of());

        int weekdayTotal = userShiftCounters.values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();

        int weekendTotal = userWeekendCounters.values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();

        return weekdayTotal + weekendTotal;
    }

    boolean isWithinRequestedWeekdayLimit(User user, int shiftType, CalculationCounters counters) {

        ShiftPreference preference = getPreference(user, shiftType);

        if (preference == null) {
            return false;
        }

        int requestedWeekdayCount = preference.getWeekdayCount();
        int currentWeekdayCount = getShiftCounter(user, shiftType, counters);

        return requestedWeekdayCount != 0
                && currentWeekdayCount < requestedWeekdayCount;
    }

    boolean isWithinRequestedWeekendLimit(User user, int shiftType, CalculationCounters counters) {

        ShiftPreference preference = getPreference(user, shiftType);

        if (preference == null) {
            return false;
        }

        int requestedWeekendCount = preference.getWeekendCount();
        int currentWeekendCount = getWeekendCounter(user, shiftType, counters);

        return requestedWeekendCount != 0
                && currentWeekendCount < requestedWeekendCount;
    }

    private ShiftPreference getPreference(User user, int shiftType) {
        if (user.getShiftRequest() == null) {
            return null;
        }

        return user.getShiftRequest()
                .getPreferences()
                .stream()
                .filter(preference -> preference.getShiftType() == shiftType)
                .findFirst()
                .orElse(null);
    }

    private int getShiftCounter(User user, int shiftType, CalculationCounters counters) {
        return counters.getWeekdayCounters()
                .getOrDefault(user.getId(), Map.of())
                .getOrDefault(shiftType, 0);
    }

    private int getWeekendCounter(User user, int shiftType, CalculationCounters counters) {
        return counters.getWeekendCounters()
                .getOrDefault(user.getId(), Map.of())
                .getOrDefault(shiftType, 0);
    }

    boolean respectsMinimalGap(LocalDate date, int minimalGap, User user, ScheduleCalendar calendar, int currentShiftType) {
        if (date == null || user == null || user.getId() == null || calendar == null) {
            return true;
        }

        LocalDate startDate = date.minusDays(minimalGap);
        LocalDate endDate = date.plusDays(minimalGap);

        for (CalendarDay day : calendar.getDays()) {

            if (day == null || day.getDate() == null || day.getAssignments() == null) {
                continue;
            }

            LocalDate checkedDate = day.getDate();

            if (checkedDate.isBefore(startDate) || checkedDate.isAfter(endDate)) {
                continue;
            }

            for (ShiftAssignment assignment : day.getAssignments()) {

                if (assignment == null || assignment.getUser() == null) {
                    continue;
                }

                boolean sameSlot =
                        checkedDate.equals(date)
                                && assignment.getShiftType() == currentShiftType;

                if (sameSlot) {
                    continue;
                }

                User assignedUser = assignment.getUser();

                if (Objects.equals(assignedUser.getId(), user.getId())) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean respectsPreviousMonthGap(
            Map<Integer, StoredCalendarDay> previousMonthCalendar,
            Integer minimalGap,
            LocalDate date,
            User user
    ) {
        if (previousMonthCalendar == null
                || minimalGap == null
                || date == null
                || user == null
                || user.getUsername() == null) {
            return true;
        }

        if (date.getDayOfMonth() - minimalGap > 0) {
            return true;
        }

        for (int backwardIndex = 0; backwardIndex > -minimalGap; backwardIndex--) {
            StoredCalendarDay previousMonthDay =
                    previousMonthCalendar.get(backwardIndex);

            if (previousMonthDay == null) {
                continue;
            }

            boolean userWorkedPreviousMonthDay =
                    previousMonthDay.getAssignmentsByShiftType()
                            .values()
                            .stream()
                            .anyMatch(snapshot ->
                                    snapshot != null
                                            && user.getUsername().equals(snapshot.getUsername())
                            );

            if (userWorkedPreviousMonthDay) {
                return false;
            }
        }

        return true;
    }


    boolean isNotRejectedByUser(LocalDate date, List<LocalDate> datesNo) {
        if (datesNo == null || date == null) {
            return true;
        }

        return !datesNo.contains(date);
    }

    boolean isValidWithinTotalShiftLimit(Integer shiftCountCap, User user, CalculationCounters counters) {
        if (user == null || !user.hasShiftRequest()) {
            return false;
        }

        if (shiftCountCap == null) {
            return true;
        }

        return counters.getTotalCount(user) <= shiftCountCap;
    }

    boolean isValidWithinRequestedWeekendLimit(User user, int shiftType, CalculationCounters counters) {
        if (user == null || !user.hasShiftRequest()) {
            return false;
        }

        ShiftPreference preference = getPreference(user, shiftType);

        int requestedWeekendCount = preference == null ? 0 : preference.getWeekendCount();

        int assignedWeekendCount = counters.getWeekendCount(user, shiftType);

        return assignedWeekendCount <= requestedWeekendCount;
    }

    boolean isValidWithinRequestedWeekdayLimit(User user, int shiftType, CalculationCounters counters) {
        if (user == null || !user.hasShiftRequest()) {
            return false;
        }

        ShiftPreference preference = getPreference(user, shiftType);

        int requestedWeekdayCount = preference == null ? 0 : preference.getWeekdayCount();

        int assignedWeekdayCount = counters.getWeekdayCount(user, shiftType);

        return assignedWeekdayCount <= requestedWeekdayCount;
    }

    public Map<Integer, StoredCalendarDay> loadPreviousMonthCalendar(
            LocalDate adminDate,
            int minimalGap
    ) {
        List<LocalDate> previousMonthDates = createPreviousMonthDatesToCheck(adminDate, minimalGap);

        Map<Integer, StoredCalendarDay> previousMonthCalendar = new HashMap<>();

        int backwardIndex = 0;

        for (LocalDate previousMonthDate : previousMonthDates) {
            Long dateId = CalendarDateIdUtils.toDateId(previousMonthDate);

            int finalBackwardIndex = backwardIndex;
            storedCalendarDayRepository.findById(dateId)
                    .ifPresent(storedDay ->
                            previousMonthCalendar.put(finalBackwardIndex, storedDay)
                    );

            backwardIndex--;
        }

        return previousMonthCalendar;
    }

    private static List<LocalDate> createPreviousMonthDatesToCheck(
            LocalDate adminDate,
            int minimalGap
    ) {
        LocalDate firstDayOfAdminMonth = adminDate.withDayOfMonth(1);

        return IntStream.rangeClosed(1, minimalGap)
                .mapToObj(firstDayOfAdminMonth::minusDays)
                .toList();
    }


}
