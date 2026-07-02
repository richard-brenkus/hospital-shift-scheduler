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
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredCalendarDay;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ScheduleMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ScheduleValidationService {

    private final UserRepository userRepository;
    private final ShiftTypeProperties shiftTypeProperties;
    private final ScheduleMapper scheduleMapper;
    private final ShiftTypeService shiftTypeService;
    private final ScheduleRuleService scheduleRuleService;

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
                .scheduleScore(returnScheduleScoreAsString(editedCalendar, shiftTypeProperties.count()))
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

        Map<Integer, Set<UserStatViewRecord>> shortStats = returnQuickUserStats(calendar, shiftCountCap, counters);

        Map<Integer, Set<UserStatViewRecord>> noShiftAssignedStats = returnNoShiftAssignedUserStatMap(calendar, counters);

        Map<Integer, Set<UserStatViewRecord>> fullStats = returnFullUserStats(calendar, counters);

        result.setShortStatsByShiftType(shortStats);
        result.setShortStatsExist(hasAnyStats(shortStats));

        result.setNoShiftAssignedStatsByShiftType(noShiftAssignedStats);
        result.setNoShiftAssignedStatsExist(hasAnyStats(noShiftAssignedStats));

        result.setFullUserStatsByShiftType(fullStats);

        result.setScheduleScore(
                returnScheduleScoreAsString(calendar, shiftTypeProperties.count())
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

    public Set<String> returnUsersWithNoRequest() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .filter(user -> !user.hasShiftRequest())
                .map(User::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Map<Integer, Set<UserStatViewRecord>> returnQuickUserStats(
            ScheduleCalendar calendar,
            int shiftCountCap,
            CalculationCounters counters
    ) {
        Map<Integer, Set<UserStatViewRecord>> userStatMap = new HashMap<>();

        if (calendar == null || calendar.getDays() == null || calendar.getMonth() == null) {
            return userStatMap;
        }

        Map<Integer, Set<Long>> alreadyAddedUserIdsByShiftType = new HashMap<>();

        for (CalendarDay day : calendar.getDays()) {
            if (day.getAssignments() == null) {
                continue;
            }

            for (ShiftAssignment assignment : day.getAssignments()) {
                User user = assignment.getUser();

                if (user == null || user.getId() == null) {
                    continue;
                }

                int shiftType = assignment.getShiftType();
                ShiftPreference preference = getPreference(user, shiftType);

                if (!preferenceAppliesToMonth(preference, calendar.getMonth())) {
                    continue;
                }

                int totalAssignedForUser = counters.getTotalCount(user);

                if (totalAssignedForUser >= shiftCountCap) {
                    continue;
                }

                int calculatedWeekdays = counters.getWeekdayCount(user, shiftType);
                int calculatedWeekends = counters.getWeekendCount(user, shiftType);

                int remainingWeekdays = preference.getWeekdayCount() - calculatedWeekdays;
                int remainingWeekends = preference.getWeekendCount() - calculatedWeekends;

                if (remainingWeekdays <= 0 && remainingWeekends <= 0) {
                    continue;
                }

                boolean alreadyAdded = alreadyAddedUserIdsByShiftType.computeIfAbsent(shiftType, key -> new HashSet<>()).contains(user.getId());

                if (alreadyAdded) {
                    continue;
                }

                UserStatViewRecord userStatViewRecord = UserStatViewRecord.builder()
                        .user(user)
                        .name(user.getName())
                        .shiftType(shiftType)
                        .requestedWeekdays(preference.getWeekdayCount())
                        .requestedWeekends(preference.getWeekendCount())
                        .calculatedWeekdays(calculatedWeekdays)
                        .calculatedWeekends(calculatedWeekends)
                        .remainingWeekdays(Math.max(remainingWeekdays, 0))
                        .remainingWeekends(Math.max(remainingWeekends, 0))
                        .anyDateSelected(preference.isAnyDateSelected())
                        .requestedDateDays(toCurrentMonthDayOfMonthSet(preference.getDatesYes(), calendar.getMonth()))
                        .assignedWeekdays(calculatedWeekdays)
                        .assignedWeekends(calculatedWeekends)
                        .assignedTotal(calculatedWeekdays + calculatedWeekends)
                        .month(calendar.getMonth())
                        .build();

                userStatMap
                        .computeIfAbsent(shiftType, key -> new HashSet<>())
                        .add(userStatViewRecord);

                alreadyAddedUserIdsByShiftType
                        .get(shiftType)
                        .add(user.getId());
            }
        }

        return userStatMap;
    }

    private boolean preferenceAppliesToMonth(ShiftPreference preference, YearMonth month) {
        if (preference == null || month == null) {
            return false;
        }

        if (preference.isAnyDateSelected()) {
            return true;
        }

        return preference.getDatesYes() != null
                && preference.getDatesYes()
                .stream()
                .anyMatch(date -> date != null && YearMonth.from(date).equals(month));
    }

    private Set<Integer> toCurrentMonthDayOfMonthSet(List<LocalDate> dates, YearMonth month) {
        if (dates == null || month == null) {
            return Set.of();
        }

        return dates.stream()
                .filter(date -> date != null && YearMonth.from(date).equals(month))
                .map(LocalDate::getDayOfMonth)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Map<Integer, Set<UserStatViewRecord>> returnNoShiftAssignedUserStatMap(ScheduleCalendar calendar, CalculationCounters counters) {
        Map<Integer, Set<UserStatViewRecord>> result = new HashMap<>();

        if (calendar == null || calendar.getMonth() == null) {
            return result;
        }

        List<User> usersWithRequest = findUsersWithRequest();
        List<Integer> shiftTypes = shiftTypeService.getShiftTypes();

        Map<Integer, Set<Long>> assignedUserIdsByShiftType = collectAssignedUserIdsByShiftType(calendar);

        for (Integer shiftType : shiftTypes) {
            Set<UserStatViewRecord> statsForShiftType = new HashSet<>();
            Set<Long> assignedUserIds =
                    assignedUserIdsByShiftType.getOrDefault(shiftType, Set.of());

            for (User user : usersWithRequest) {
                if (user.getId() == null) {
                    continue;
                }

                if (!user.getAllowedShiftTypes().contains(shiftType)) {
                    continue;
                }

                ShiftPreference preference = getPreference(user, shiftType);

                if (preference == null || preference.isNoShiftRequested()) {
                    continue;
                }

                boolean assignedForThisShiftType =
                        assignedUserIds.contains(user.getId());

                if (assignedForThisShiftType) {
                    continue;
                }

                boolean notAssignedAnywhere =
                        counters.getTotalCount(user) == 0;

                if (!notAssignedAnywhere) {
                    continue;
                }

                UserStatViewRecord stat = UserStatViewRecord.builder()
                        .user(user)
                        .name(user.getName())
                        .shiftType(shiftType)
                        .requestedWeekdays(preference.getWeekdayCount())
                        .requestedWeekends(preference.getWeekendCount())
                        .anyDateSelected(preference.isAnyDateSelected())
                        .requestedDateDays(toCurrentMonthDayOfMonthSet(preference.getDatesYes(), calendar.getMonth()))
                        .assignedWeekdays(0)
                        .assignedWeekends(0)
                        .assignedTotal(0)
                        .month(calendar.getMonth())
                        .build();

                statsForShiftType.add(stat);
            }

            result.put(shiftType, statsForShiftType);
        }

        return result;
    }

    private Map<Integer, Set<UserStatViewRecord>> returnFullUserStats(
            ScheduleCalendar calendar,
            CalculationCounters counters
    ) {
        Map<Integer, Map<Long, UserStatBuilderData>> statsByShiftTypeAndUser =
                new HashMap<>();

        if (calendar == null || calendar.getDays() == null) {
            return Map.of();
        }

        for (CalendarDay day : calendar.getDays()) {
            if (day.getAssignments() == null) {
                continue;
            }

            int dayOfMonth = day.getDate().getDayOfMonth();

            for (ShiftAssignment assignment : day.getAssignments()) {
                User user = assignment.getUser();

                if (user == null || user.getId() == null) {
                    continue;
                }

                int shiftType = assignment.getShiftType();

                ShiftPreference preference = getPreference(user, shiftType);

                int requestedWeekdays = preference == null ? 0 : preference.getWeekdayCount();
                int requestedWeekends = preference == null ? 0 : preference.getWeekendCount();
                boolean anyDateSelected = preference == null || preference.isAnyDateSelected();

                int assignedWeekdays = counters.getWeekdayCount(user, shiftType);
                int assignedWeekends = counters.getWeekendCount(user, shiftType);

                UserStatBuilderData builderData =
                        statsByShiftTypeAndUser
                                .computeIfAbsent(shiftType, key -> new HashMap<>())
                                .computeIfAbsent(user.getId(), key ->
                                        UserStatBuilderData.builder()
                                                .user(user)
                                                .name(user.getName())
                                                .shiftType(shiftType)
                                                .requestedWeekdays(requestedWeekdays)
                                                .requestedWeekends(requestedWeekends)
                                                .calculatedWeekdays(assignedWeekdays)
                                                .calculatedWeekends(assignedWeekends)
                                                .assignedWeekdays(assignedWeekdays)
                                                .assignedWeekends(assignedWeekends)
                                                .assignedTotal(assignedWeekdays + assignedWeekends)
                                                .remainingWeekdays(Math.max(requestedWeekdays - assignedWeekdays, 0))
                                                .remainingWeekends(Math.max(requestedWeekends - assignedWeekends, 0))
                                                .anyDateSelected(anyDateSelected)
                                                .requestedDateDays(preference == null
                                                        ? Set.of()
                                                        : toCurrentMonthDayOfMonthSet(preference.getDatesYes(), calendar.getMonth()))
                                                .assignedDateDays(new TreeSet<>())
                                                .month(calendar.getMonth())
                                                .build()
                                );

                builderData.assignedDateDays().add(dayOfMonth);
            }
        }

        Map<Integer, Set<UserStatViewRecord>> result = new HashMap<>();

        for (Map.Entry<Integer, Map<Long, UserStatBuilderData>> shiftTypeEntry
                : statsByShiftTypeAndUser.entrySet()) {

            Integer shiftType = shiftTypeEntry.getKey();

            Set<UserStatViewRecord> statsForShiftType =
                    shiftTypeEntry.getValue()
                            .values()
                            .stream()
                            .map(UserStatBuilderData::toUserStat)
                            .collect(Collectors.toCollection(HashSet::new));

            result.put(shiftType, statsForShiftType);
        }

        return addAllShiftsUserStat(result);
    }

    private Map<Integer, Set<UserStatViewRecord>> addAllShiftsUserStat(
            Map<Integer, Set<UserStatViewRecord>> fullUserStatMap
    ) {
        Map<String, Integer> totalAssignedByUserName = new HashMap<>();

        for (Set<UserStatViewRecord> stats : fullUserStatMap.values()) {
            for (UserStatViewRecord stat : stats) {
                totalAssignedByUserName.merge(
                        stat.name(),
                        stat.assignedTotal(),
                        Integer::sum
                );
            }
        }

        Map<Integer, Set<UserStatViewRecord>> result = new HashMap<>();

        for (Map.Entry<Integer, Set<UserStatViewRecord>> entry : fullUserStatMap.entrySet()) {
            Integer shiftType = entry.getKey();

            Set<UserStatViewRecord> updatedStats = entry.getValue()
                    .stream()
                    .map(stat -> stat.withAssignedTotalAllShiftTypes(
                            totalAssignedByUserName.getOrDefault(stat.name(), 0)
                    ))
                    .collect(Collectors.toCollection(HashSet::new));

            result.put(shiftType, updatedStats);
        }

        return result;
    }

    private Map<Integer, Set<Long>> collectAssignedUserIdsByShiftType(
            ScheduleCalendar calendar
    ) {
        Map<Integer, Set<Long>> result = new HashMap<>();

        if (calendar == null || calendar.getDays() == null) {
            return result;
        }

        for (CalendarDay day : calendar.getDays()) {
            if (day.getAssignments() == null) {
                continue;
            }

            for (ShiftAssignment assignment : day.getAssignments()) {
                User user = assignment.getUser();

                if (user == null || user.getId() == null) {
                    continue;
                }

                result.computeIfAbsent(
                        assignment.getShiftType(),
                        key -> new HashSet<>()
                ).add(user.getId());
            }
        }

        return result;
    }

    private List<User> findUsersWithRequest() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .filter(User::hasShiftRequest)
                .toList();
    }

    private String returnScheduleScoreAsString(
            ScheduleCalendar calendar,
            int shiftTypeCount
    ) {
        if (calendar == null || calendar.getDays() == null) {
            return "0/0";
        }

        int assignedCount = 0;

        for (CalendarDay day : calendar.getDays()) {
            if (day.getAssignments() == null) {
                continue;
            }

            for (ShiftAssignment assignment : day.getAssignments()) {
                User user = assignment.getUser();

                if (user != null && user.getId() != null) {
                    assignedCount++;
                }
            }
        }

        if (calendar.getMonth() == null) {
            return assignedCount + "/0";
        }

        int maxScore = calendar.getMonth().lengthOfMonth() * shiftTypeCount;

        return assignedCount + "/" + maxScore;
    }

    @Builder
    private record UserStatBuilderData(
            User user,
            String name,
            int shiftType,
            int requestedWeekdays,
            int requestedWeekends,
            int calculatedWeekdays,
            int calculatedWeekends,
            int remainingWeekdays,
            int remainingWeekends,
            boolean anyDateSelected,
            Set<Integer> requestedDateDays,
            int assignedWeekdays,
            int assignedWeekends,
            int assignedTotal,
            Set<Integer> assignedDateDays,
            YearMonth month
    ) {
        UserStatViewRecord toUserStat() {
            return UserStatViewRecord.builder()
                    .user(user)
                    .name(name)
                    .shiftType(shiftType)
                    .requestedWeekdays(requestedWeekdays)
                    .requestedWeekends(requestedWeekends)
                    .calculatedWeekdays(calculatedWeekdays)
                    .calculatedWeekends(calculatedWeekends)
                    .remainingWeekdays(remainingWeekdays)
                    .remainingWeekends(remainingWeekends)
                    .anyDateSelected(anyDateSelected)
                    .requestedDateDays(requestedDateDays)
                    .assignedWeekdays(assignedWeekdays)
                    .assignedWeekends(assignedWeekends)
                    .assignedTotal(assignedTotal)
                    .assignedDateDays(assignedDateDays)
                    .month(month)
                    .build();
        }
    }
}
