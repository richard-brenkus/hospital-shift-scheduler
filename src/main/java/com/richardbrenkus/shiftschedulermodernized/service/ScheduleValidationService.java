package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalendarDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.PreviousMonthShiftRecord;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleCalendar;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleValidationResult;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.algorithm.UserStat;
import com.richardbrenkus.shiftschedulermodernized.config.ShiftTypeProperties;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ScheduleEditForm;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredCalendarDay;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ScheduleMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredCalendarDayRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.util.CalendarDateIdUtils;
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
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ScheduleValidationService {

    private final UserRepository userRepository;
    private final StoredCalendarDayRepository storedCalendarDayRepository;
    private final ShiftTypeProperties shiftTypeProperties;
    private final StoredScheduleService storedScheduleService;
    private final ScheduleMapper scheduleMapper;
    private final ShiftTypeService shiftTypeService;

    /**
     * Use this after calculateSchedule(...), before the admin edits the generated table.
     * It produces only statistics/display data, not validation errors.
     */
    @Transactional(readOnly = true)
    public ScheduleValidationResult generateUserStats(ScheduleCalendar calendar) {
        ScheduleValidationResult result = ScheduleValidationResult.builder()
                .calendar(calendar)
                .allUsersExist(true)
                .errorsExist(false)
                .build();

        return generateUserStats(calendar, result);
    }

    /**
     * Use this after the admin submits the edited schedule.
     * It validates the edited calendar and also refreshes user statistics.
     */
    @Transactional
    public ScheduleValidationResult evaluateEdit(ScheduleEditForm scheduleEditForm, boolean saveSchedule) {
        ScheduleCalendar editedCalendar = scheduleMapper.toScheduleCalendar(scheduleEditForm, scheduleEditForm.toCalculationProfileForm());

        ScheduleValidationResult result = validateSchedule(editedCalendar);
        generateUserStats(editedCalendar, result);

        /*Set<String> usersWithNoRequest = returnUsersWithNoRequest();
        result.setUsersWithNoRequest(usersWithNoRequest);
        result.setUsersWithNoRequestString(String.join(", ", usersWithNoRequest));*/

        if (saveSchedule && !result.isErrorsExist()) {
            storedScheduleService.saveSchedule(editedCalendar);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public ScheduleValidationResult validateSchedule(ScheduleCalendar calendar) {

        if (calendar == null) {
            return ScheduleValidationResult.builder()
                    .allUsersExist(true)
                    .errorsExist(false)
                    .build();
        }

        CalculationProfileForm profile = calendar.getCalculationProfile();

        if (profile == null) {
            throw new IllegalArgumentException("Schedule calendar has no calculation profile.");
        }

        int shiftCountCap = profile.getShiftCountCap();
        int minimalGap = profile.getGapBetweenShifts();

        Set<Integer> forceFillShiftTypes = profile.getForceFillShiftTypes() == null
                ? Set.of()
                : new HashSet<>(profile.getForceFillShiftTypes());

        Map<Integer, PreviousMonthShiftRecord> previousMonthCalendar = loadPreviousMonthCalendar(calendar.getMonth().atDay(1), minimalGap, shiftTypeService.getShiftTypes());

        CalculationCounters counters = countAssignments(calendar);

        ScheduleValidationResult result = ScheduleValidationResult.builder()
                .calendar(calendar)
                .allUsersExist(true)
                .errorsExist(false)
                .scheduleScore(returnScheduleScoreAsString(calendar, shiftTypeProperties.count()))
                .build();

        if (calendar.getDays() == null) {
            return result;
        }

        for (CalendarDay day : calendar.getDays()) {

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

                if (!calendar.isOverrideHasShiftRequest() && !user.hasShiftRequest()) {
                    markError(result, shiftType, day);
                    result.setUserNoRequest(true);
                    result.addUserNoRequest(shiftType, userName);
                    continue;
                }

                if (!user.hasShiftRequest()) {
                    continue;
                }

                ShiftRequest shiftRequest = user.getShiftRequest();

                boolean shiftCountOk = shiftCountCapValidator(shiftCountCap, user, counters);

                boolean weekdayCapOk = true;
                boolean weekendCapOk = true;

                if (day.isWeekendOrHoliday()) {
                    weekendCapOk = weekendCapValidator(user, shiftType, counters);
                } else {
                    weekdayCapOk = userShiftCountValidator(user, shiftType, counters);
                }

                boolean crossCheckOk = crossValidator(day.getDate(), minimalGap, user, calendar, shiftType);

                boolean datesNoOk = datesNoValidator(day.getDate(), shiftRequest.getDatesNo());

                boolean previousMonthOk = checkPreviousMonth(previousMonthCalendar, minimalGap, day.getDate(), user);

                if (!shiftCountOk && !calendar.isOverrideShiftCountCap()) {
                    markError(result, shiftType, day);
                    result.setUserShiftCap(true);
                    result.addShiftCapUser(shiftType, userName);
                }

                boolean forceFill = forceFillShiftTypes.contains(shiftType);

                if (!forceFill
                        && !calendar.isOverrideUserShiftRequestExceptNoDates()
                        && !calendar.isOverrideUserShiftRequestAll()) {

                    if (!weekdayCapOk) {
                        markError(result, shiftType, day);
                        result.setUserIndividualShiftCap(true);
                        result.addIndividualShiftCapUser(shiftType, userName);
                    }

                    if (!weekendCapOk) {
                        markError(result, shiftType, day);
                        result.setUserWeekendCap(true);
                        result.addWeekendCapUser(shiftType, userName);
                    }
                }

                if (!crossCheckOk && !calendar.isOverrideConflictingDates()) {
                    markError(result, shiftType, day);
                    result.setUserCrossCheck(true);
                    result.addCrossCheckUser(shiftType, userName);
                }

                if (!datesNoOk && !calendar.isOverrideUserShiftRequestAll()) {
                    markError(result, shiftType, day);
                    result.setUserDatesNo(true);
                    result.addDatesNoCheckUser(shiftType, userName);
                }

                if (!previousMonthOk && !calendar.isOverridePreviousMonthValid()) {
                    markError(result, shiftType, day);
                    result.setPreviousMonthCheckFailed(true);
                    result.addPreviousMonthCheckUser(shiftType, userName);
                }
            }
        }

        return result;
    }

    private ScheduleValidationResult generateUserStats(ScheduleCalendar calendar, ScheduleValidationResult result) {
        if (calendar == null || calendar.getCalculationProfile() == null) {
            return result;
        }

        CalculationCounters counters = countAssignments(calendar);
        int shiftCountCap = calendar.getCalculationProfile().getShiftCountCap();

        Map<Integer, Set<UserStat>> shortStats = returnQuickUserStats(calendar, shiftCountCap, counters);

        Map<Integer, Set<UserStat>> noShiftAssignedStats = returnNoShiftAssignedUserStatMap(calendar, counters);

        Map<Integer, Set<UserStat>> fullStats = returnFullUserStats(calendar, counters);

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

    private boolean hasAnyStats(Map<Integer, Set<UserStat>> stats) {
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

    private boolean datesNoValidator(LocalDate date, List<LocalDate> datesNo) {
        if (datesNo == null || date == null) {
            return true;
        }

        return !datesNo.contains(date);
    }

    private boolean shiftCountCapValidator(Integer shiftCountCap, User user, CalculationCounters counters) {
        if (user == null || !user.hasShiftRequest()) {
            return false;
        }

        if (shiftCountCap == null) {
            return true;
        }

        return counters.getTotalCount(user) <= shiftCountCap;
    }

    private boolean weekendCapValidator(User user, int shiftType, CalculationCounters counters) {
        if (user == null || !user.hasShiftRequest()) {
            return false;
        }

        ShiftPreference preference = getPreference(user, shiftType);

        int requestedWeekendCount = preference == null ? 0 : preference.getWeekendCount();

        int assignedWeekendCount = counters.getWeekendCount(user, shiftType);

        return assignedWeekendCount <= requestedWeekendCount;
    }

    private boolean userShiftCountValidator(User user, int shiftType, CalculationCounters counters) {
        if (user == null || !user.hasShiftRequest()) {
            return false;
        }

        ShiftPreference preference = getPreference(user, shiftType);

        int requestedWeekdayCount = preference == null ? 0 : preference.getWeekdayCount();

        int assignedWeekdayCount = counters.getWeekdayCount(user, shiftType);

        return assignedWeekdayCount <= requestedWeekdayCount;
    }

    boolean crossValidator(LocalDate date, int minimalGap, User user, ScheduleCalendar calendar, int currentShiftType) {
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

    boolean checkPreviousMonth(
            Map<Integer, PreviousMonthShiftRecord> previousMonthCalendar,
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

        int dayOfMonth = date.getDayOfMonth();

        if (dayOfMonth - minimalGap > 0) {
            return true;
        }

        for (int backwardIndex = 0; backwardIndex > -minimalGap; backwardIndex--) {
            PreviousMonthShiftRecord previousMonthDay =
                    previousMonthCalendar.get(backwardIndex);

            if (previousMonthDay != null
                    && previousMonthDay.containsUsername(user.getUsername())) {
                return false;
            }
        }

        return true;
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

    Map<Integer, PreviousMonthShiftRecord> loadPreviousMonthCalendar(
            LocalDate adminDate,
            int minimalGap,
            List<Integer> shiftTypes
    ) {
        List<LocalDate> previousMonthDates = createPreviousMonthDatesToCheck(adminDate, minimalGap);

        Map<Integer, PreviousMonthShiftRecord> previousMonthCalendar = new HashMap<>();

        int backwardIndex = 0;

        for (LocalDate previousMonthDate : previousMonthDates) {
            Long dateId = CalendarDateIdUtils.toDateId(previousMonthDate);

            StoredCalendarDay storedCalendarDay =
                    storedCalendarDayRepository.findById(dateId).orElse(null);

            if (storedCalendarDay != null) {
                PreviousMonthShiftRecord previousMonthShiftRecord =
                        PreviousMonthShiftRecord.builder()
                                .backwardIndex(backwardIndex)
                                .dateStringDB(String.valueOf(dateId))
                                .dateIdDB(dateId)
                                .usernameByShiftType(new HashMap<>())
                                .build();

                for (Integer shiftType : shiftTypes) {
                    String username = storedCalendarDay.getUsernameForShiftType(shiftType);
                    previousMonthShiftRecord.setUsernameForShiftType(shiftType, username);
                }

                previousMonthCalendar.put(backwardIndex, previousMonthShiftRecord);
            }

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

    public Set<String> returnUsersWithNoRequest() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .filter(user -> !user.hasShiftRequest())
                .map(User::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Map<Integer, Set<UserStat>> returnQuickUserStats(
            ScheduleCalendar calendar,
            int shiftCountCap,
            CalculationCounters counters
    ) {
        Map<Integer, Set<UserStat>> userStatMap = new HashMap<>();

        if (calendar == null || calendar.getDays() == null) {
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

                if (preference == null) {
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

                boolean alreadyAdded =
                        alreadyAddedUserIdsByShiftType
                                .computeIfAbsent(shiftType, key -> new HashSet<>())
                                .contains(user.getId());

                if (alreadyAdded) {
                    continue;
                }

                UserStat userStat = UserStat.builder()
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
                        .requestedDateDays(toDayOfMonthSet(preference.getDatesYes()))
                        .assignedWeekdays(calculatedWeekdays)
                        .assignedWeekends(calculatedWeekends)
                        .assignedTotal(calculatedWeekdays + calculatedWeekends)
                        .month(calendar.getMonth())
                        .build();

                userStatMap
                        .computeIfAbsent(shiftType, key -> new HashSet<>())
                        .add(userStat);

                alreadyAddedUserIdsByShiftType
                        .get(shiftType)
                        .add(user.getId());
            }
        }

        return userStatMap;
    }

    private Map<Integer, Set<UserStat>> returnNoShiftAssignedUserStatMap(
            ScheduleCalendar calendar,
            CalculationCounters counters
    ) {
        Map<Integer, Set<UserStat>> result = new HashMap<>();

        if (calendar == null || calendar.getMonth() == null) {
            return result;
        }

        List<User> usersWithRequest = findUsersWithRequest();
        List<Integer> shiftTypes = shiftTypeService.getShiftTypes();

        Map<Integer, Set<Long>> assignedUserIdsByShiftType = collectAssignedUserIdsByShiftType(calendar);

        for (Integer shiftType : shiftTypes) {
            Set<UserStat> statsForShiftType = new HashSet<>();
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

                UserStat stat = UserStat.builder()
                        .user(user)
                        .name(user.getName())
                        .shiftType(shiftType)
                        .requestedWeekdays(preference.getWeekdayCount())
                        .requestedWeekends(preference.getWeekendCount())
                        .anyDateSelected(preference.isAnyDateSelected())
                        .requestedDateDays(toDayOfMonthSet(preference.getDatesYes()))
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

    private Map<Integer, Set<UserStat>> returnFullUserStats(
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
                                                        : toDayOfMonthSet(preference.getDatesYes()))
                                                .assignedDateDays(new TreeSet<>())
                                                .month(calendar.getMonth())
                                                .build()
                                );

                builderData.assignedDateDays().add(dayOfMonth);
            }
        }

        Map<Integer, Set<UserStat>> result = new HashMap<>();

        for (Map.Entry<Integer, Map<Long, UserStatBuilderData>> shiftTypeEntry
                : statsByShiftTypeAndUser.entrySet()) {

            Integer shiftType = shiftTypeEntry.getKey();

            Set<UserStat> statsForShiftType =
                    shiftTypeEntry.getValue()
                            .values()
                            .stream()
                            .map(UserStatBuilderData::toUserStat)
                            .collect(Collectors.toCollection(HashSet::new));

            result.put(shiftType, statsForShiftType);
        }

        return addAllShiftsUserStat(result);
    }

    private Map<Integer, Set<UserStat>> addAllShiftsUserStat(
            Map<Integer, Set<UserStat>> fullUserStatMap
    ) {
        Map<String, Integer> totalAssignedByUserName = new HashMap<>();

        for (Set<UserStat> stats : fullUserStatMap.values()) {
            for (UserStat stat : stats) {
                totalAssignedByUserName.merge(
                        stat.name(),
                        stat.assignedTotal(),
                        Integer::sum
                );
            }
        }

        Map<Integer, Set<UserStat>> result = new HashMap<>();

        for (Map.Entry<Integer, Set<UserStat>> entry : fullUserStatMap.entrySet()) {
            Integer shiftType = entry.getKey();

            Set<UserStat> updatedStats = entry.getValue()
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

    private Set<Integer> toDayOfMonthSet(List<LocalDate> dates) {
        if (dates == null) {
            return Set.of();
        }

        return dates.stream()
                .map(LocalDate::getDayOfMonth)
                .collect(Collectors.toCollection(TreeSet::new));
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
        UserStat toUserStat() {
            return UserStat.builder()
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
