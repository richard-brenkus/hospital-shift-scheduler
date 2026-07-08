package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.algorithm.UsersForShiftType;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredScheduleDay;
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
            ScheduleMonth scheduleMonth,
            List<Integer> monthDays,
            List<Integer> priorities,
            List<Integer> calculationOrder,
            Map<Integer, UsersForShiftType> usersByShiftType,
            List<Integer> forceFillShiftTypes,
            boolean sortByDatesAmount,
            int shiftCountCap,
            int minimalGap,
            Map<Integer, StoredScheduleDay> previousMonthStoredScheduleDays,
            CalculationCounters counters
    ) {
        int hitCounter = 0;

        for (Integer dayOfMonth : monthDays) {
            ScheduleDay scheduleDay = getScheduleDay(scheduleMonth, scheduleMonth.getMonth().atDay(dayOfMonth));

            for (Integer priority : priorities) {
                for (Integer shiftType : calculationOrder) {
                    if (!forceFillShiftTypes.contains(shiftType)) {
                        continue;
                    }

                    List<User> orderedUsers = getUsersInCalculationOrder(
                            usersByShiftType.get(shiftType),
                            shiftType,
                            sortByDatesAmount,
                            scheduleMonth.getMonth()
                    );

                    boolean assigned = tryAssignUser(
                            AssignmentMode.FORCE_FILL,
                            scheduleMonth,
                            scheduleDay,
                            orderedUsers,
                            shiftType,
                            priority,
                            shiftCountCap,
                            minimalGap,
                            previousMonthStoredScheduleDays,
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
            ScheduleMonth scheduleMonth,
            List<Integer> monthDays,
            List<Integer> priorities,
            List<Integer> calculationOrder,
            Map<Integer, UsersForShiftType> usersByShiftType,
            List<Integer> forceFillShiftTypes,
            boolean sortByDatesAmount,
            int shiftCountCap,
            int minimalGap,
            Map<Integer, StoredScheduleDay> previousMonthStoredScheduleDays,
            CalculationCounters counters
    ) {
        int hitCounter = 0;

        for (Integer dayOfMonth : monthDays) {
            ScheduleDay scheduleDay = getScheduleDay(scheduleMonth, scheduleMonth.getMonth().atDay(dayOfMonth));

            for (Integer priority : priorities) {
                for (Integer shiftType : calculationOrder) {
                    if (forceFillShiftTypes.contains(shiftType)) {
                        continue;
                    }

                    List<User> orderedUsers = getUsersInCalculationOrder(
                            usersByShiftType.get(shiftType),
                            shiftType,
                            sortByDatesAmount,
                            scheduleMonth.getMonth()
                    );

                    boolean assigned = tryAssignUser(
                            AssignmentMode.REGULAR,
                            scheduleMonth,
                            scheduleDay,
                            orderedUsers,
                            shiftType,
                            priority,
                            shiftCountCap,
                            minimalGap,
                            previousMonthStoredScheduleDays,
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
            ScheduleMonth scheduleMonth,
            ScheduleDay scheduleDay,
            List<User> users,
            int shiftType,
            int priority,
            int shiftCountCap,
            int minimalGap,
            Map<Integer, StoredScheduleDay> previousMonthSchedule,
            CalculationCounters counters
    ) {
        if (hasAssignment(scheduleDay, shiftType)) {
            return false;
        }

        for (User user : users) {
            ShiftPreference preference = getPreference(user, shiftType);

            if (preference == null || preference.getPriority() != priority) {
                continue;
            }

            if (!appliesToCalculationMonth(preference, scheduleMonth.getMonth())) {
                continue;
            }

            if (!isEligibleForAssignmentMode(assignmentMode, user, preference, scheduleDay)) {
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
                            scheduleDay.getDate(),
                            minimalGap,
                            user,
                            scheduleMonth,
                            shiftType
                    );

            boolean respectsPreviousMonthGap =
                    scheduleRuleService.respectsPreviousMonthGap(
                            previousMonthSchedule,
                            minimalGap,
                            scheduleDay.getDate(),
                            user
                    );

            if (!withinTotalShiftLimit || !respectsMinimalGap || !respectsPreviousMonthGap) {
                continue;
            }

            if (!scheduleDay.isWeekendOrHoliday() && withinRequestedWeekdayLimit) {
                incrementWeekdayCounter(user, shiftType, counters);
                addAssignment(scheduleDay, shiftType, user);
                return true;
            }

            if (scheduleDay.isWeekendOrHoliday() && withinRequestedWeekendLimit) {
                incrementWeekendCounter(user, shiftType, counters);
                addAssignment(scheduleDay, shiftType, user);
                return true;
            }
        }

        return false;
    }

    private boolean isEligibleForAssignmentMode(
            AssignmentMode assignmentMode,
            User user,
            ShiftPreference preference,
            ScheduleDay scheduleDay
    ) {
        ShiftRequest request = user.getShiftRequest();

        if (request == null) {
            return false;
        }

        boolean dateRejected = containsDay(request.getDatesNo(), scheduleDay.getDate());

        if (dateRejected) {
            return false;
        }

        if (assignmentMode == AssignmentMode.FORCE_FILL) {
            return true;
        }

        boolean dateExplicitlySelected = containsDay(preference.getDatesYes(), scheduleDay.getDate());
        boolean anyDateSelected = preference.isAnyDateSelected();

        return dateExplicitlySelected || anyDateSelected;
    }

    private ScheduleDay getScheduleDay(ScheduleMonth scheduleMonth, LocalDate date) {
        return scheduleMonth.getDays()
                .stream()
                .filter(day -> day.getDate().equals(date))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Schedule day not found: " + date));
    }

    private boolean hasAssignment(ScheduleDay day, int shiftType) {
        return day.getAssignments()
                .stream()
                .anyMatch(assignment -> assignment.getShiftType() == shiftType);
    }

    private void addAssignment(ScheduleDay day, int shiftType, User user) {
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
