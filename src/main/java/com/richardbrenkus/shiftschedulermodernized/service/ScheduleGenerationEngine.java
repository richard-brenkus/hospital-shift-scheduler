package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculationInput;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ShiftPreferenceCalculationData;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.UserCalculationData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleGenerationEngine {
    private final ScheduleRuleService scheduleRuleService;

    public int assignForceFillShifts(CalculatedScheduleMonth scheduleMonth, List<Integer> monthDays, CalculationInput input, CalculationCounters counters, Random random) {
        return assignShifts(AssignmentMode.FORCE_FILL, scheduleMonth, monthDays, input, counters, random);
    }

    public int assignRegularShifts(CalculatedScheduleMonth scheduleMonth, List<Integer> monthDays, CalculationInput input, CalculationCounters counters, Random random) {
        return assignShifts(AssignmentMode.REGULAR, scheduleMonth, monthDays, input, counters, random);
    }

    private int assignShifts(AssignmentMode mode, CalculatedScheduleMonth scheduleMonth, List<Integer> monthDays, CalculationInput input, CalculationCounters counters, Random random) {
        int hitCounter = 0;
        Map<Integer, List<UserCalculationData>> usersByShiftType = prepareUsersByShiftType(input);

        for (Integer dayOfMonth : monthDays) {
            CalculatedScheduleDay scheduleDay = getScheduleDay(scheduleMonth, input.month().atDay(dayOfMonth));

            for (Integer priority : input.priorities()) {
                for (Integer shiftType : input.calculationOrder()) {
                    boolean forceFill = input.profile().forceFillShiftTypes().contains(shiftType);
                    if (mode == AssignmentMode.FORCE_FILL && !forceFill) continue;
                    if (mode == AssignmentMode.REGULAR && forceFill) continue;

                    List<UserCalculationData> orderedUsers = getUsersInCalculationOrder(usersByShiftType.get(shiftType), shiftType, input.profile().sortByDatesAmount(), input, random);

                    if (tryAssignUser(mode, scheduleMonth, scheduleDay, orderedUsers, shiftType, priority, input, counters)) {
                        hitCounter++;
                    }
                }
            }
        }
        return hitCounter;
    }

    private boolean tryAssignUser(AssignmentMode mode, CalculatedScheduleMonth scheduleMonth, CalculatedScheduleDay scheduleDay, List<UserCalculationData> users, int shiftType, int priority, CalculationInput input, CalculationCounters counters) {
        if (hasAssignment(scheduleDay, shiftType)) return false;

        for (UserCalculationData user : users) {
            ShiftPreferenceCalculationData preference = user.preferenceFor(shiftType).orElse(null);

            if (preference == null || preference.priority() != priority || !preference.appliesToMonth(input.month()) || !isEligibleForAssignmentMode(mode, user, preference, scheduleDay)) {
                continue;
            }

            boolean withinTotalLimit = scheduleRuleService.isWithinTotalShiftLimit(input.profile().shiftCountCap(), user, counters);
            boolean withinWeekdayLimit = scheduleRuleService.isWithinRequestedWeekdayLimit(user, shiftType, counters);
            boolean withinWeekendLimit = scheduleRuleService.isWithinRequestedWeekendLimit(user, shiftType, counters);
            boolean respectsCurrentMonthGap = scheduleRuleService.respectsMinimalGap(scheduleDay.getDate(), input.profile().gapBetweenShifts(), user, scheduleMonth, shiftType);
            boolean respectsPreviousMonthGap = scheduleRuleService.respectsPreviousMonthGap(input.profile().gapBetweenShifts(), scheduleDay.getDate(), user, input.month());

            if (!withinTotalLimit || !respectsCurrentMonthGap || !respectsPreviousMonthGap) continue;

            if (!scheduleDay.isWeekendOrHoliday() && withinWeekdayLimit) {
                counters.incrementWeekday(user.userId(), shiftType);
                addAssignment(scheduleDay, shiftType, user.userId());
                return true;
            }

            if (scheduleDay.isWeekendOrHoliday() && withinWeekendLimit) {
                counters.incrementWeekend(user.userId(), shiftType);
                addAssignment(scheduleDay, shiftType, user.userId());
                return true;
            }
        }
        return false;
    }

    private boolean isEligibleForAssignmentMode(AssignmentMode mode, UserCalculationData user, ShiftPreferenceCalculationData preference, CalculatedScheduleDay scheduleDay) {
        if (user.isUnavailableOn(scheduleDay.getDate())) return false;
        return mode == AssignmentMode.FORCE_FILL || preference.acceptsDate(scheduleDay.getDate());
    }

    private Map<Integer, List<UserCalculationData>> prepareUsersByShiftType(CalculationInput input) {
        return input.shiftTypes().stream().collect(Collectors.toUnmodifiableMap(Function.identity(), shiftType -> input.users().stream().filter(user -> user.canWorkShiftType(shiftType)).filter(user -> user.preferenceFor(shiftType).filter(preference -> !preference.noShiftRequested()).isPresent()).toList()));
    }

    private List<UserCalculationData> getUsersInCalculationOrder(List<UserCalculationData> users, int shiftType, boolean sortByDatesAmount, CalculationInput input, Random random) {
        if (users == null || users.isEmpty()) return List.of();

        List<UserCalculationData> specificDateUsers = users.stream().filter(user -> user.preferenceFor(shiftType).map(preference -> !preference.anyDateSelected() && preference.appliesToMonth(input.month())).orElse(false)).collect(Collectors.toCollection(ArrayList::new));

        List<UserCalculationData> anyDateUsers = users.stream().filter(user -> user.preferenceFor(shiftType).map(ShiftPreferenceCalculationData::anyDateSelected).orElse(false)).collect(Collectors.toCollection(ArrayList::new));

        if (sortByDatesAmount) {
            specificDateUsers.sort(Comparator.comparingInt(user -> user.preferenceFor(shiftType).map(preference -> {
                int count = preference.requestedDatesInMonth(input.month());
                return count == 0 ? Integer.MAX_VALUE : count;
            }).orElse(Integer.MAX_VALUE)));
        } else {
            Collections.shuffle(specificDateUsers, random);
        }

        Collections.shuffle(anyDateUsers, random);
        List<UserCalculationData> result = new ArrayList<>(specificDateUsers.size() + anyDateUsers.size());
        result.addAll(specificDateUsers);
        result.addAll(anyDateUsers);
        return result;
    }

    private CalculatedScheduleDay getScheduleDay(CalculatedScheduleMonth scheduleMonth, LocalDate date) {
        return scheduleMonth.getDays().stream().filter(day -> day.getDate().equals(date)).findFirst().orElseThrow(() -> new IllegalStateException("Schedule day not found: " + date));
    }

    private boolean hasAssignment(CalculatedScheduleDay day, int shiftType) {
        return day.getAssignments().stream().anyMatch(assignment -> assignment.shiftType() == shiftType);
    }

    private void addAssignment(CalculatedScheduleDay day, int shiftType, Long userId) {
        day.getAssignments().add(new CalculatedShiftAssignment(shiftType, userId));
    }

    private enum AssignmentMode {
        FORCE_FILL, REGULAR
    }
}
