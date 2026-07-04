package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalendarDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleCalendar;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleValidationResult;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserStatViewRecord;
import com.richardbrenkus.shiftschedulermodernized.config.ShiftTypeProperties;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ScheduleEditForm;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredCalendarDay;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ScheduleMapper;
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
    private final ScheduleMapper scheduleMapper;
    private final ScheduleRuleService scheduleRuleService;
    private final UserStatisticService userStatisticService;

    @Transactional(readOnly = true)
    public ScheduleValidationResult initializeValidationAndUserStats(ScheduleCalendar calendar) {
        ScheduleValidationResult result = ScheduleValidationResult.builder()
                .calendar(calendar)
                .allUsersExist(true)
                .errorsExist(false)
                .build();

        return generateValidationAndUserStats(calendar, result);
    }

    @Transactional(readOnly = true)
    public ScheduleValidationResult validateSchedule(ScheduleEditForm scheduleEditForm) {
        ScheduleCalendar editedCalendar = scheduleMapper.toScheduleCalendar(scheduleEditForm, scheduleEditForm.toCalculationProfileForm());

        if (editedCalendar == null) {
            return ScheduleValidationResult.builder()
                    .allUsersExist(true)
                    .errorsExist(false)
                    .build();
        }

        CalculationProfileForm profile = editedCalendar.getCalculationProfile();

        if (profile == null) {
            throw new IllegalArgumentException("Schedule calendar has no calculation profile.");
        }

        int shiftCountCap = profile.getShiftCountCap();
        int minimalGap = profile.getGapBetweenShifts();

        Set<Integer> forceFillShiftTypes = profile.getForceFillShiftTypes() == null
                ? Set.of()
                : new HashSet<>(profile.getForceFillShiftTypes());

        Map<Integer, StoredCalendarDay> previousMonthCalendar = scheduleRuleService.loadPreviousMonthCalendar(editedCalendar.getMonth().atDay(1), minimalGap);

        CalculationCounters counters = countAssignments(editedCalendar);

        ScheduleValidationResult result = ScheduleValidationResult.builder()
                .calendar(editedCalendar)
                .allUsersExist(true)
                .errorsExist(false)
                .scheduleScore(userStatisticService.returnScheduleScoreAsString(editedCalendar, shiftTypeProperties.count()))
                .build();

        if (editedCalendar.getDays() == null) {
            return result;
        }

        for (CalendarDay day : editedCalendar.getDays()) {

            if (day == null || day.getAssignments() == null) {
                continue;
            }

            for (ShiftAssignment assignment : day.getAssignments()) {

                if (assignment == null) {
                    continue;
                }

                User user = assignment.getUser();

                if (user == null || user.getId() == null) {
                    continue;
                }

                int shiftType = assignment.getShiftType();
                String userName = user.getName();

                if (!editedCalendar.isOverrideHasShiftRequest() && !user.hasShiftRequest()) {
                    markError(result, shiftType, day);
                    result.setUserNoRequest(true);
                    result.addUserNoRequest(shiftType, userName);
                    continue;
                }

                if (!user.hasShiftRequest()) {
                    continue;
                }

                ShiftRequest shiftRequest = user.getShiftRequest();

                boolean withinTotalShiftLimit = scheduleRuleService.isValidWithinTotalShiftLimit(shiftCountCap, user, counters);

                boolean withinRequestedWeekdayLimit = true;
                boolean withinRequestedWeekendLimit = true;

                if (day.isWeekendOrHoliday()) {
                    withinRequestedWeekendLimit = scheduleRuleService.isValidWithinRequestedWeekendLimit(user, shiftType, counters);
                } else {
                    withinRequestedWeekdayLimit = scheduleRuleService.isValidWithinRequestedWeekdayLimit(user, shiftType, counters);
                }

                boolean respectsMinimalGap = scheduleRuleService.respectsMinimalGap(day.getDate(), minimalGap, user, editedCalendar, shiftType);

                boolean isNotRejectedByUser = scheduleRuleService.isNotRejectedByUser(day.getDate(), shiftRequest.getDatesNo());

                boolean respectsPreviousMonthGap = scheduleRuleService.respectsPreviousMonthGap(previousMonthCalendar, minimalGap, day.getDate(), user);

                if (!withinTotalShiftLimit && !editedCalendar.isOverrideShiftCountCap()) {
                    markError(result, shiftType, day);
                    result.setUserShiftCap(true);
                    result.addShiftCapUser(shiftType, userName);
                }

                boolean forceFill = forceFillShiftTypes.contains(shiftType);

                if (!forceFill
                        && !editedCalendar.isOverrideUserShiftRequestExceptNoDates()
                        && !editedCalendar.isOverrideUserShiftRequestAll()) {

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

                if (!respectsMinimalGap && !editedCalendar.isOverrideConflictingDates()) {
                    markError(result, shiftType, day);
                    result.setUserCrossCheck(true);
                    result.addCrossCheckUser(shiftType, userName);
                }

                if (!isNotRejectedByUser && !editedCalendar.isOverrideUserShiftRequestAll()) {
                    markError(result, shiftType, day);
                    result.setUserDatesNo(true);
                    result.addDatesNoCheckUser(shiftType, userName);
                }

                if (!respectsPreviousMonthGap && !editedCalendar.isOverridePreviousMonthValid()) {
                    markError(result, shiftType, day);
                    result.setPreviousMonthCheckFailed(true);
                    result.addPreviousMonthCheckUser(shiftType, userName);
                }
            }
        }

        this.generateValidationAndUserStats(editedCalendar, result);

        return result;
    }

    private ScheduleValidationResult generateValidationAndUserStats(ScheduleCalendar calendar, ScheduleValidationResult result) {
        if (calendar == null || calendar.getCalculationProfile() == null) {
            return result;
        }

        CalculationCounters counters = countAssignments(calendar);
        int shiftCountCap = calendar.getCalculationProfile().getShiftCountCap();

        Map<Integer, Set<UserStatViewRecord>> shortStats = userStatisticService.returnQuickUserStats(calendar, shiftCountCap, counters);

        Map<Integer, Set<UserStatViewRecord>> noShiftAssignedStats = userStatisticService.returnNoShiftAssignedUserStatMap(calendar, counters);

        Map<Integer, Set<UserStatViewRecord>> fullStats = userStatisticService.returnFullUserStats(calendar, counters);

        result.setShortStatsByShiftType(shortStats);
        result.setShortStatsExist(hasAnyStats(shortStats));

        result.setNoShiftAssignedStatsByShiftType(noShiftAssignedStats);
        result.setNoShiftAssignedStatsExist(hasAnyStats(noShiftAssignedStats));

        result.setFullUserStatsByShiftType(fullStats);

        result.setScheduleScore(userStatisticService.returnScheduleScoreAsString(calendar, shiftTypeProperties.count())
        );

        return result;
    }

    private void markError(ScheduleValidationResult result, int shiftType, CalendarDay day) {
        result.setErrorsExist(true);
        result.markRedField(shiftType, day.getDate().getDayOfMonth());
    }

    private boolean hasAnyStats(Map<Integer, Set<UserStatViewRecord>> stats) {
        return stats != null
                && stats.values().stream()
                .anyMatch(values -> values != null && !values.isEmpty());
    }

    private CalculationCounters countAssignments(ScheduleCalendar calendar) {
        CalculationCounters counters = new CalculationCounters();

        if (calendar == null || calendar.getDays() == null) {
            return counters;
        }

        for (CalendarDay day : calendar.getDays()) {
            if (day == null || day.getAssignments() == null) {
                continue;
            }

            for (ShiftAssignment assignment : day.getAssignments()) {
                User user = assignment.getUser();

                if (user == null || user.getId() == null) {
                    continue;
                }

                int shiftType = assignment.getShiftType();

                if (day.isWeekendOrHoliday()) {
                    counters.incrementWeekend(user, shiftType);
                } else {
                    counters.incrementWeekday(user, shiftType);
                }
            }
        }

        return counters;
    }
}
