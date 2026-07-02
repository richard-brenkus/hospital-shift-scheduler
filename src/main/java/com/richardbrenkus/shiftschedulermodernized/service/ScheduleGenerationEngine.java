package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalendarDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleCalendar;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.algorithm.UsersForShiftType;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredCalendarDay;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScheduleGenerationEngine {

    private final ScheduleRuleService scheduleRuleService;

    public int assignForceFillShifts(
            ScheduleCalendar calendar,
            List<Integer> monthDays,
            List<Integer> priorities,
            List<Integer> calculationOrder,
            Map<Integer, UsersForShiftType> usersByShiftType,
            List<Integer> forceFillShiftTypes,
            boolean sortByDatesAmount,
            int shiftCountCap,
            int minimalGap,
            Map<Integer, StoredCalendarDay> previousMonthCalendar,
            CalculationCounters counters
    ) {
        int hitCounter = 0;

        for (Integer dayOfMonth : monthDays) {
            CalendarDay calendarDay = getCalendarDay(calendar, calendar.getMonth().atDay(dayOfMonth));

            for (Integer priority : priorities) {
                for (Integer shiftType : calculationOrder) {
                    if (!forceFillShiftTypes.contains(shiftType)) {
                        continue;
                    }

                    List<User> orderedUsers = getUsersInCalculationOrder(
                            usersByShiftType.get(shiftType),
                            shiftType,
                            sortByDatesAmount,
                            calendar.getMonth()
                    );

                    boolean assigned = tryAssignUser(
                            AssignmentMode.FORCE_FILL,
                            calendar,
                            calendarDay,
                            orderedUsers,
                            shiftType,
                            priority,
                            shiftCountCap,
                            minimalGap,
                            previousMonthCalendar,
                            counters
                    );

                    if (assigned) {
                        hitCounter++;
                    }
                }
            }
        }

        return hitCounter;
    }

    public int assignRegularShifts(
            ScheduleCalendar calendar,
            List<Integer> monthDays,
            List<Integer> priorities,
            List<Integer> calculationOrder,
            Map<Integer, UsersForShiftType> usersByShiftType,
            List<Integer> forceFillShiftTypes,
            boolean sortByDatesAmount,
            int shiftCountCap,
            int minimalGap,
            Map<Integer, StoredCalendarDay> previousMonthCalendar,
            CalculationCounters counters
    ) {
        int hitCounter = 0;

        for (Integer dayOfMonth : monthDays) {
            CalendarDay calendarDay = getCalendarDay(calendar, calendar.getMonth().atDay(dayOfMonth));

            for (Integer priority : priorities) {
                for (Integer shiftType : calculationOrder) {
                    if (forceFillShiftTypes.contains(shiftType)) {
                        continue;
                    }

                    List<User> orderedUsers = getUsersInCalculationOrder(
                            usersByShiftType.get(shiftType),
                            shiftType,
                            sortByDatesAmount,
                            calendar.getMonth()
                    );

                    boolean assigned = tryAssignUser(
                            AssignmentMode.REGULAR,
                            calendar,
                            calendarDay,
                            orderedUsers,
                            shiftType,
                            priority,
                            shiftCountCap,
                            minimalGap,
                            previousMonthCalendar,
                            counters
                    );

                    if (assigned) {
                        hitCounter++;
                    }
                }
            }
        }

        return hitCounter;
    }

    private boolean tryAssignUser(
            AssignmentMode assignmentMode,
            ScheduleCalendar calendar,
            CalendarDay calendarDay,
            List<User> users,
            int shiftType,
            int priority,
            int shiftCountCap,
            int minimalGap,
            Map<Integer, StoredCalendarDay> previousMonthCalendar,
            CalculationCounters counters
    ) {
        if (hasAssignment(calendarDay, shiftType)) {
            return false;
        }

        for (User user : users) {
            ShiftPreference preference = getPreference(user, shiftType);

            if (preference == null || preference.getPriority() != priority) {
                continue;
            }

            if (!appliesToCalculationMonth(preference, calendar.getMonth())) {
                continue;
            }

            if (!isEligibleForAssignmentMode(assignmentMode, user, preference, calendarDay)) {
                continue;
            }

            boolean withinTotalShiftLimit =
                    scheduleRuleService.isWithinTotalShiftLimit(shiftCountCap, user, counters);

            boolean withinRequestedWeekdayLimit =
                    scheduleRuleService.isWithinRequestedWeekdayLimit(user, shiftType, counters);

            boolean withinRequestedWeekendLimit =
                    scheduleRuleService.isWithinRequestedWeekendLimit(user, shiftType, counters);

            boolean respectsMinimalGap =
                    scheduleRuleService.respectsMinimalGap(
                            calendarDay.getDate(),
                            minimalGap,
                            user,
                            calendar,
                            shiftType
                    );

            boolean respectsPreviousMonthGap =
                    scheduleRuleService.respectsPreviousMonthGap(
                            previousMonthCalendar,
                            minimalGap,
                            calendarDay.getDate(),
                            user
                    );

            if (!withinTotalShiftLimit || !respectsMinimalGap || !respectsPreviousMonthGap) {
                continue;
            }

            if (!calendarDay.isWeekendOrHoliday() && withinRequestedWeekdayLimit) {
                incrementWeekdayCounter(user, shiftType, counters);
                addAssignment(calendarDay, shiftType, user);
                return true;
            }

            if (calendarDay.isWeekendOrHoliday() && withinRequestedWeekendLimit) {
                incrementWeekendCounter(user, shiftType, counters);
                addAssignment(calendarDay, shiftType, user);
                return true;
            }
        }

        return false;
    }

    private boolean isEligibleForAssignmentMode(
            AssignmentMode assignmentMode,
            User user,
            ShiftPreference preference,
            CalendarDay calendarDay
    ) {
        ShiftRequest request = user.getShiftRequest();

        if (request == null) {
            return false;
        }

        boolean dateRejected = containsDay(request.getDatesNo(), calendarDay.getDate());

        if (dateRejected) {
            return false;
        }

        if (assignmentMode == AssignmentMode.FORCE_FILL) {
            return true;
        }

        boolean dateExplicitlySelected = containsDay(preference.getDatesYes(), calendarDay.getDate());
        boolean anyDateSelected = preference.isAnyDateSelected();

        return dateExplicitlySelected || anyDateSelected;
    }

    private CalendarDay getCalendarDay(ScheduleCalendar calendar, LocalDate date) {
        return calendar.getDays()
                .stream()
                .filter(day -> day.getDate().equals(date))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Calendar day not found: " + date));
    }

    private boolean hasAssignment(CalendarDay day, int shiftType) {
        return day.getAssignments()
                .stream()
                .anyMatch(assignment -> assignment.getShiftType() == shiftType);
    }

    private void addAssignment(CalendarDay day, int shiftType, User user) {
        day.getAssignments().add(
                ShiftAssignment.builder()
                        .shiftType(shiftType)
                        .user(user)
                        .build()
        );
    }

    private void incrementWeekdayCounter(User user, int shiftType, CalculationCounters counters) {
        counters.getWeekdayCounters()
                .computeIfAbsent(user.getId(), id -> new java.util.HashMap<>())
                .merge(shiftType, 1, Integer::sum);
    }

    private void incrementWeekendCounter(User user, int shiftType, CalculationCounters counters) {
        counters.getWeekendCounters()
                .computeIfAbsent(user.getId(), id -> new java.util.HashMap<>())
                .merge(shiftType, 1, Integer::sum);
    }

    private ShiftPreference getPreference(User user, int shiftType) {
        if (user == null || user.getShiftRequest() == null) {
            return null;
        }

        return user.getShiftRequest()
                .getPreferences()
                .stream()
                .filter(preference -> preference.getShiftType() == shiftType)
                .findFirst()
                .orElse(null);
    }

    private boolean containsDay(List<LocalDate> dates, LocalDate targetDate) {
        if (dates == null || targetDate == null) {
            return false;
        }

        return dates.contains(targetDate);
    }

    private List<User> getUsersInCalculationOrder(
            UsersForShiftType users,
            int shiftType,
            boolean sortByDatesAmount,
            YearMonth calculationMonth
    ) {
        if (users == null) {
            return List.of();
        }

        List<User> specificDateUsers = new ArrayList<>(
                users.specificDateUsers() == null ? List.of() : users.specificDateUsers()
        );

        List<User> anyDateUsers = new ArrayList<>(
                users.anyDateUsers() == null ? List.of() : users.anyDateUsers()
        );

        if (sortByDatesAmount) {
            specificDateUsers.sort(
                    Comparator.comparingInt(user -> {
                        ShiftPreference preference = getPreference(user, shiftType);
                        return countRequestedDatesInMonth(preference, calculationMonth);
                    })
            );
        } else {
            Collections.shuffle(specificDateUsers);
        }

        Collections.shuffle(anyDateUsers);

        List<User> result = new ArrayList<>();
        result.addAll(specificDateUsers);
        result.addAll(anyDateUsers);

        return result;
    }

    private boolean appliesToCalculationMonth(ShiftPreference preference, YearMonth calculationMonth) {
        if (preference == null) {
            return false;
        }

        if (preference.isAnyDateSelected()) {
            return true;
        }

        return hasRequestedDateInMonth(preference, calculationMonth);
    }

    private boolean hasRequestedDateInMonth(ShiftPreference preference, YearMonth calculationMonth) {
        if (preference == null || preference.getDatesYes() == null || calculationMonth == null) {
            return false;
        }

        return preference.getDatesYes()
                .stream()
                .anyMatch(date -> date != null && YearMonth.from(date).equals(calculationMonth));
    }

    private int countRequestedDatesInMonth(ShiftPreference preference, YearMonth calculationMonth) {
        if (preference == null || preference.getDatesYes() == null || calculationMonth == null) {
            return Integer.MAX_VALUE;
        }

        long count = preference.getDatesYes()
                .stream()
                .filter(date -> date != null && YearMonth.from(date).equals(calculationMonth))
                .count();

        return count == 0 ? Integer.MAX_VALUE : Math.toIntExact(count);
    }

    private enum AssignmentMode {
        FORCE_FILL,
        REGULAR
    }
}
