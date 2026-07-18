package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.*;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ScheduleEditForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.*;
import com.richardbrenkus.shiftschedulermodernized.entity.*;
import com.richardbrenkus.shiftschedulermodernized.mapper.ScheduleMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;

import com.richardbrenkus.shiftschedulermodernized.repository.UserStatRepository;
import jakarta.servlet.http.HttpSession;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class UserStatisticService {

    private static final String SESSION_FULL_USER_STATS = "fullUserStatsByShiftType";
    private static final String SESSION_FULL_STATS_MONTH = "fullStatsMonth";

    private final UserRepository userRepository;
    private final ShiftTypeService shiftTypeService;
    private final ScheduleMapper scheduleMapper;
    private final UserStatRepository userStatRepository;

    public void storeFullStatisticsInSession(HttpSession session, ScheduleValidationResult validationResult, ScheduleEditForm scheduleEditForm) {
        if (session == null) {
            return;
        }

        if (validationResult == null) {
            clearFullStatistics(session);
            return;
        }

        Map<Integer, Set<UserStatViewRecord>> fullStats = validationResult.getFullUserStatsByShiftType() == null
                ? Map.of()
                : validationResult.getFullUserStatsByShiftType();

        session.setAttribute(SESSION_FULL_USER_STATS, fullStats);

        if (scheduleEditForm != null && scheduleEditForm.getMonth() != null) {
            session.setAttribute(SESSION_FULL_STATS_MONTH, scheduleEditForm.getMonth());
        }
    }

    public void addFullStatisticsToModel(Model model, HttpSession session, List<Integer> shiftTypes) {
        Map<Integer, Set<UserStatViewRecord>> fullUserStatsByShiftType = getFullUserStatsFromSession(session);

        YearMonth month = getFullStatsMonthFromSession(session);

        boolean statsExist = fullUserStatsByShiftType.values()
                .stream()
                .anyMatch(stats -> stats != null && !stats.isEmpty());

        model.addAttribute("fullUserStatsByShiftType", fullUserStatsByShiftType);

        model.addAttribute("shiftTypes", shiftTypes);
        model.addAttribute("statsExist", statsExist);

        if (month != null) {
            model.addAttribute("month", month);
            model.addAttribute("year", month.getYear());
            model.addAttribute("monthInt", month.getMonthValue());
        }
    }

    public void clearFullStatistics(HttpSession session) {
        if (session == null) {
            return;
        }

        session.removeAttribute(SESSION_FULL_USER_STATS);
        session.removeAttribute(SESSION_FULL_STATS_MONTH);
    }


    Map<Integer, Set<UserStatViewRecord>> returnQuickUserStats(ScheduleMonth scheduleMonth, int shiftCountCap, CalculationCounters counters) {
        Map<Integer, Set<UserStatViewRecord>> userStatMap = new HashMap<>();

        if (scheduleMonth == null || scheduleMonth.getDays() == null || scheduleMonth.getMonth() == null) {
            return userStatMap;
        }

        Map<Integer, Set<Long>> alreadyAddedUserIdsByShiftType = new HashMap<>();

        for (ScheduleDay day : scheduleMonth.getDays()) {
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

                if (!preferenceAppliesToMonth(preference, scheduleMonth.getMonth())) {
                    continue;
                }

                int totalAssignedForUser = counters.getTotalCount(user.getId());

                if (totalAssignedForUser >= shiftCountCap) {
                    continue;
                }

                int calculatedWeekdays = counters.getWeekdayCount(user.getId(), shiftType);
                int calculatedWeekends = counters.getWeekendCount(user.getId(), shiftType);

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
                        .requestedDateDays(toCurrentMonthDayOfMonthSet(preference.getDatesYes(), scheduleMonth.getMonth()))
                        .assignedWeekdays(calculatedWeekdays)
                        .assignedWeekends(calculatedWeekends)
                        .assignedTotal(calculatedWeekdays + calculatedWeekends)
                        .month(scheduleMonth.getMonth())
                        .allowedShiftTypesAsCommaSeparatedString(this.allowedShiftTypesToCommaSeparatedString(user))
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

    Set<Integer> toCurrentMonthDayOfMonthSet(List<LocalDate> dates, YearMonth month) {
        if (dates == null || month == null) {
            return Set.of();
        }

        return dates.stream()
                .filter(date -> date != null && YearMonth.from(date).equals(month))
                .map(LocalDate::getDayOfMonth)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    Map<Integer, Set<UserStatViewRecord>> returnNoShiftAssignedUserStatMap(ScheduleMonth scheduleMonth, CalculationCounters counters) {
        Map<Integer, Set<UserStatViewRecord>> result = new HashMap<>();

        if (scheduleMonth == null || scheduleMonth.getMonth() == null) {
            return result;
        }

        List<User> usersWithRequest = findUsersWithRequest();
        List<Integer> shiftTypes = shiftTypeService.getShiftTypes();

        Map<Integer, Set<Long>> assignedUserIdsByShiftType = collectAssignedUserIdsByShiftType(scheduleMonth);

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
                        counters.getTotalCount(user.getId()) == 0;

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
                        .requestedDateDays(toCurrentMonthDayOfMonthSet(preference.getDatesYes(), scheduleMonth.getMonth()))
                        .assignedWeekdays(0)
                        .assignedWeekends(0)
                        .assignedTotal(0)
                        .month(scheduleMonth.getMonth())
                        .build();

                statsForShiftType.add(stat);
            }

            result.put(shiftType, statsForShiftType);
        }

        return result;
    }

    public Map<Integer, Set<UserStatViewRecord>> returnFullUserStats(ScheduleMonth scheduleMonth, CalculationCounters counters) {
        Map<Integer, Map<Long, UserStatBuilderData>> statsByShiftTypeAndUser =
                new HashMap<>();

        if (scheduleMonth == null || scheduleMonth.getDays() == null) {
            return Map.of();
        }

        for (ScheduleDay day : scheduleMonth.getDays()) {
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

                int assignedWeekdays = counters.getWeekdayCount(user.getId(), shiftType);
                int assignedWeekends = counters.getWeekendCount(user.getId(), shiftType);

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
                                                        : toCurrentMonthDayOfMonthSet(preference.getDatesYes(), scheduleMonth.getMonth()))
                                                .assignedDateDays(new TreeSet<>())
                                                .month(scheduleMonth.getMonth())
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

    public Set<String> returnUsersWithNoRequest() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .filter(user -> !user.hasShiftRequest())
                .map(User::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Transactional
    public void replaceStatsForMonth(
            YearMonth yearMonth,
            Map<Integer, Set<UserStatViewRecord>> statsByShiftType
    ) {
        if (yearMonth == null) {
            throw new IllegalArgumentException("YearMonth must not be null");
        }

        userStatRepository.deleteByYearMonth(yearMonth);

        if (statsByShiftType == null || statsByShiftType.isEmpty()) {
            return;
        }

        List<UserStatEntity> entities = statsByShiftType.values()
                .stream()
                .flatMap(Set::stream)
                .map(scheduleMapper::toUserStatEntity)
                .peek(entity -> entity.setYearMonth(yearMonth))
                .toList();

        userStatRepository.saveAll(entities);
    }


    @Transactional(readOnly = true)
    public Map<Integer, Set<UserStatViewRecord>> findViewRecordsByYearMonth(YearMonth yearMonth) {
        if (yearMonth == null) {
            return Map.of();
        }

        return userStatRepository.findByYearMonthOrderByShiftTypeAscNameAsc(yearMonth)
                .stream()
                .map(scheduleMapper::toUserStatViewRecord)
                .collect(Collectors.groupingBy(
                        UserStatViewRecord::shiftType,
                        Collectors.toCollection(LinkedHashSet::new)
                ));
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

    private Map<Integer, Set<Long>> collectAssignedUserIdsByShiftType(ScheduleMonth scheduleMonth) {
        Map<Integer, Set<Long>> result = new HashMap<>();

        if (scheduleMonth == null || scheduleMonth.getDays() == null) {
            return result;
        }

        for (ScheduleDay day : scheduleMonth.getDays()) {
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

    String returnScheduleScoreAsString(ScheduleMonth scheduleMonth, int shiftTypeCount) {
        if (scheduleMonth == null || scheduleMonth.getDays() == null) {
            return "0/0";
        }

        int assignedCount = 0;

        for (ScheduleDay day : scheduleMonth.getDays()) {
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

        if (scheduleMonth.getMonth() == null) {
            return assignedCount + "/0";
        }

        int maxScore = scheduleMonth.getMonth().lengthOfMonth() * shiftTypeCount;

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


    @SuppressWarnings("unchecked")
    private Map<Integer, Set<UserStatViewRecord>> getFullUserStatsFromSession(HttpSession session) {
        if (session == null) {
            return Map.of();
        }

        Object value = session.getAttribute(SESSION_FULL_USER_STATS);

        if (!(value instanceof Map<?, ?>)) {
            return Map.of();
        }

        return (Map<Integer, Set<UserStatViewRecord>>) value;
    }

    private YearMonth getFullStatsMonthFromSession(HttpSession session) {
        if (session == null) {
            return null;
        }

        Object value = session.getAttribute(SESSION_FULL_STATS_MONTH);

        if (value instanceof YearMonth yearMonth) {
            return yearMonth;
        }

        return null;
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

    private String allowedShiftTypesToCommaSeparatedString(User user) {
        return user.getAllowedShiftTypes().stream()
                .map(shiftType -> Integer.toString(shiftType))
                .collect(Collectors.joining(", "));
    }
}
