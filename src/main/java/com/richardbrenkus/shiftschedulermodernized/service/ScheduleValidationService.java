package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleValidationResult;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.UserCalculationData;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserStatViewRecord;
import com.richardbrenkus.shiftschedulermodernized.config.ShiftTypeProperties;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ScheduleEditForm;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredScheduleDay;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScheduleValidationService {

    private final ShiftTypeProperties shiftTypeProperties;
    private final ScheduleRuleService scheduleRuleService;
    private final UserStatisticService userStatisticService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public ScheduleValidationResult initializeValidationAndUserStats(ScheduleMonth scheduleMonth) {
        ScheduleValidationResult result = ScheduleValidationResult.builder()
                .scheduleMonth(scheduleMonth)
                .allUsersExist(true)
                .errorsExist(false)
                .build();

        return generateValidationAndUserStats(scheduleMonth, result);
    }

    @Transactional(readOnly = true)
    public ScheduleValidationResult validateSchedule(ScheduleEditForm scheduleEditForm) {

        ScheduleMonth editedScheduleMonth = userService.getScheduleMonth(scheduleEditForm);

        if (editedScheduleMonth == null) {
            return ScheduleValidationResult.builder()
                    .allUsersExist(true)
                    .errorsExist(false)
                    .build();
        }

        CalculationProfileForm profile = editedScheduleMonth.getCalculationProfile();

        if (profile == null) {
            throw new IllegalArgumentException("Schedule has no calculation profile.");
        }

        int shiftCountCap = profile.getShiftCountCap();
        int minimalGap = profile.getGapBetweenShifts();

        Set<Integer> forceFillShiftTypes = profile.getForceFillShiftTypes() == null
                ? Set.of()
                : new HashSet<>(profile.getForceFillShiftTypes());

        Map<Integer, StoredScheduleDay> previousMonthStoredScheduleDays = scheduleRuleService.loadPreviousStoredScheduleDays(editedScheduleMonth.getMonth().atDay(1), minimalGap);

        CalculationCounters counters = countAssignments(editedScheduleMonth);

        ScheduleValidationResult result = ScheduleValidationResult.builder()
                .scheduleMonth(editedScheduleMonth)
                .allUsersExist(true)
                .errorsExist(false)
                .scheduleScore(userStatisticService.returnScheduleScoreAsString(editedScheduleMonth, shiftTypeProperties.count()))
                .build();

        if (editedScheduleMonth.getDays() == null) {
            return result;
        }

        for (ScheduleDay day : editedScheduleMonth.getDays()) {

            if (day == null || day.getAssignments() == null) {
                continue;
            }

            for (ShiftAssignment assignment : day.getAssignments()) {

                if (assignment == null) {
                    continue;
                }

                UserCalculationData userCalculationData = assignment.getUserCalculationData();

                if (userCalculationData == null || userCalculationData.userId() == null) {
                    continue;
                }

                int shiftType = assignment.getShiftType();
                String userName = userCalculationData.username();

                if (!editedScheduleMonth.isOverrideHasShiftRequest() && !userCalculationData.hasShiftRequest()) {
                    markError(result, shiftType, day);
                    result.setUserNoRequest(true);
                    result.addUserNoRequest(shiftType, userName);
                    continue;
                }

                if (!userCalculationData.hasShiftRequest()) {
                    continue;
                }

                boolean withinTotalShiftLimit = scheduleRuleService.isValidWithinTotalShiftLimit(shiftCountCap, userCalculationData, counters);

                boolean withinRequestedWeekdayLimit = true;
                boolean withinRequestedWeekendLimit = true;

                if (day.isWeekendOrHoliday()) {
                    withinRequestedWeekendLimit = scheduleRuleService.isValidWithinRequestedWeekendLimit(userCalculationData, shiftType, counters);
                } else {
                    withinRequestedWeekdayLimit = scheduleRuleService.isValidWithinRequestedWeekdayLimit(userCalculationData, shiftType, counters);
                }

                boolean respectsMinimalGap = scheduleRuleService.respectsMinimalGap(day.getDate(), minimalGap, userCalculationData, editedScheduleMonth, shiftType);

                boolean isNotRejectedByUser = scheduleRuleService.isNotRejectedByUser(day.getDate(), userCalculationData.unavailableDates());

                boolean respectsPreviousMonthGap = scheduleRuleService.respectsPreviousMonthGap(previousMonthStoredScheduleDays, minimalGap, day.getDate(), userCalculationData);

                if (!withinTotalShiftLimit && !editedScheduleMonth.isOverrideShiftCountCap()) {
                    markError(result, shiftType, day);
                    result.setUserShiftCap(true);
                    result.addShiftCapUser(shiftType, userName);
                }

                boolean forceFill = forceFillShiftTypes.contains(shiftType);

                if (!forceFill
                        && !editedScheduleMonth.isOverrideUserShiftRequestExceptNoDates()
                        && !editedScheduleMonth.isOverrideUserShiftRequestAll()) {

                    if (!withinRequestedWeekdayLimit) {
                        markError(result, shiftType, day);
                        result.setUserIndividualShiftCap(true);
                        result.addIndividualShiftCapUser(shiftType, userName);
                    }

                    if (!withinRequestedWeekendLimit) {
                        markError(result, shiftType, day);
                        result.setUserWeekendCap(true);
                        result.addWeekendCapUser(shiftType, userName);
                    }
                }

                if (!respectsMinimalGap && !editedScheduleMonth.isOverrideConflictingDates()) {
                    markError(result, shiftType, day);
                    result.setUserCrossCheck(true);
                    result.addCrossCheckUser(shiftType, userName);
                }

                if (!isNotRejectedByUser && !editedScheduleMonth.isOverrideUserShiftRequestAll()) {
                    markError(result, shiftType, day);
                    result.setUserDatesNo(true);
                    result.addDatesNoCheckUser(shiftType, userName);
                }

                if (!respectsPreviousMonthGap && !editedScheduleMonth.isOverridePreviousMonthValid()) {
                    markError(result, shiftType, day);
                    result.setPreviousMonthCheckFailed(true);
                    result.addPreviousMonthCheckUser(shiftType, userName);
                }
            }
        }

        this.generateValidationAndUserStats(editedScheduleMonth, result);

        return result;
    }

    private ScheduleValidationResult generateValidationAndUserStats(ScheduleMonth scheduleMonth, ScheduleValidationResult result) {
        if (scheduleMonth == null || scheduleMonth.getCalculationProfile() == null) {
            return result;
        }

        CalculationCounters counters = countAssignments(scheduleMonth);
        int shiftCountCap = scheduleMonth.getCalculationProfile().getShiftCountCap();

        Map<Integer, Set<UserStatViewRecord>> shortStats = userStatisticService.returnQuickUserStats(scheduleMonth, shiftCountCap, counters);

        Map<Integer, Set<UserStatViewRecord>> noShiftAssignedStats = userStatisticService.returnNoShiftAssignedUserStatMap(scheduleMonth, counters);

        Map<Integer, Set<UserStatViewRecord>> fullStats = userStatisticService.returnFullUserStats(scheduleMonth, counters);

        result.setShortStatsByShiftType(shortStats);
        result.setShortStatsExist(hasAnyStats(shortStats));

        result.setNoShiftAssignedStatsByShiftType(noShiftAssignedStats);
        result.setNoShiftAssignedStatsExist(hasAnyStats(noShiftAssignedStats));

        result.setFullUserStatsByShiftType(fullStats);

        result.setScheduleScore(userStatisticService.returnScheduleScoreAsString(scheduleMonth, shiftTypeProperties.count())
        );

        return result;
    }

    private void markError(ScheduleValidationResult result, int shiftType, ScheduleDay day) {
        result.setErrorsExist(true);
        result.markRedField(shiftType, day.getDate().getDayOfMonth());
    }

    private boolean hasAnyStats(Map<Integer, Set<UserStatViewRecord>> stats) {
        return stats != null
                && stats.values().stream()
                .anyMatch(values -> values != null && !values.isEmpty());
    }

    private CalculationCounters countAssignments(ScheduleMonth scheduleMonth) {
        CalculationCounters counters = new CalculationCounters();

        if (scheduleMonth == null || scheduleMonth.getDays() == null) {
            return counters;
        }

        for (ScheduleDay day : scheduleMonth.getDays()) {
            if (day == null || day.getAssignments() == null) {
                continue;
            }

            for (ShiftAssignment assignment : day.getAssignments()) {

                if(assignment == null)
                    continue;

                UserCalculationData userCalculationData = assignment.getUserCalculationData();

                if (userCalculationData == null || userCalculationData.userId() == null) {
                    continue;
                }

                int shiftType = assignment.getShiftType();

                if (day.isWeekendOrHoliday()) {
                    counters.incrementWeekend(userCalculationData.userId(), shiftType);
                } else {
                    counters.incrementWeekday(userCalculationData.userId(), shiftType);
                }
            }
        }

        return counters;
    }
}
