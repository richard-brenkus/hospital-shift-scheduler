package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalendarDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.PreviousMonthShiftRecord;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleCalendar;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.config.ShiftTypeProperties;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredCalendarDay;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredCalendarDayRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ScheduleCalculationService {

    private static final int NUMBER_OF_TRIES = 100;

    private final UserRepository userRepository;
    private final StoredCalendarDayRepository storedCalendarDayRepository;
    private final ShiftTypeProperties shiftTypeProperties;

    private final Map<Long, Map<Integer, Integer>> shiftCounters = new HashMap<>();
    private final Map<Long, Map<Integer, Integer>> weekendCounters = new HashMap<>();

    public ScheduleCalendar calculateSchedule(CalculationProfileForm form) {

        boolean sortByDatesAmount = form.isSortByDatesAmount();

        int minimalGap = form.getGapBetweenShifts();
        int shiftCountCap = form.getShiftCountCap();

        YearMonth calculationMonth = form.getCalculationMonth();

        List<Integer> shiftTypes = getShiftTypes();
        List<Integer> calculationOrder = resolveShiftCalculationOrder(form, shiftTypes);
        List<Integer> priorities = IntStream.rangeClosed(1, 10)
                .boxed()
                .toList();

        List<User> usersWithRequest = findUsersWithRequest();

        LocalDate firstDayOfMonth = calculationMonth.atDay(1);
        List<LocalDate> holidaysCzech = getHolidaysCzechRepublic(form.getCalculationMonth());

        Map<Integer, PreviousMonthShiftRecord> previousMonthCalendar =
                loadPreviousMonthCalendar(firstDayOfMonth, minimalGap, shiftTypes);

        List<ScheduleCalendar> candidateCalendars = new ArrayList<>();

        for (int attempt = 0; attempt < NUMBER_OF_TRIES; attempt++) {

            ScheduleCalendar calendar = createEmptyScheduleCalendar(
                    form,
                    calculationMonth,
                    holidaysCzech
            );

            List<Integer> monthDays = IntStream.rangeClosed(1, calculationMonth.lengthOfMonth())
                    .boxed()
                    .collect(Collectors.toCollection(ArrayList::new));

            Collections.shuffle(monthDays);

            Map<Integer, List<User>> usersByShiftType =
                    prepareUsersByShiftType(usersWithRequest, shiftTypes, sortByDatesAmount);

            int hitCounter = 0;

            for (Integer dayOfMonth : monthDays) {

                LocalDate date = calculationMonth.atDay(dayOfMonth);
                CalendarDay calendarDay = getCalendarDay(calendar, date);

                reshuffleUsersByShiftType(usersByShiftType, sortByDatesAmount);

                for (Integer priority : priorities) {

                    for (Integer shiftType : calculationOrder) {

                        boolean forceFill = form.getForceFillShiftTypes() != null
                                && form.getForceFillShiftTypes().contains(shiftType);

                        if (forceFill) {
                            boolean assigned = tryAssignForceFillShift(
                                    calendar,
                                    calendarDay,
                                    usersByShiftType.getOrDefault(shiftType, List.of()),
                                    shiftType,
                                    priority,
                                    shiftCountCap,
                                    minimalGap,
                                    previousMonthCalendar
                            );

                            if (assigned) {
                                hitCounter++;
                            }
                        }
                    }
                }
            }

            for (Integer dayOfMonth : monthDays) {

                LocalDate date = calculationMonth.atDay(dayOfMonth);
                CalendarDay calendarDay = getCalendarDay(calendar, date);

                reshuffleUsersByShiftType(usersByShiftType, sortByDatesAmount);

                for (Integer priority : priorities) {

                    for (Integer shiftType : calculationOrder) {

                        boolean forceFill = form.getForceFillShiftTypes() != null
                                && form.getForceFillShiftTypes().contains(shiftType);

                        if (!forceFill) {
                            boolean assigned = tryAssignNormalShift(
                                    calendar,
                                    calendarDay,
                                    usersByShiftType.getOrDefault(shiftType, List.of()),
                                    shiftType,
                                    priority,
                                    shiftCountCap,
                                    minimalGap,
                                    previousMonthCalendar
                            );

                            if (assigned) {
                                hitCounter++;
                            }
                        }
                    }
                }
            }

            resetTemporaryCounters(usersWithRequest);

            calendar.setHitCounter(hitCounter);
            candidateCalendars.add(calendar);
        }

        return candidateCalendars.stream()
                .max(Comparator.comparingInt(ScheduleCalendar::getHitCounter))
                .orElseThrow(() -> new IllegalStateException("No schedule calendar was created"));
    }

    private boolean tryAssignForceFillShift(
            ScheduleCalendar calendar,
            CalendarDay calendarDay,
            List<User> users,
            int shiftType,
            int priority,
            int shiftCountCap,
            int minimalGap,
            Map<Integer, PreviousMonthShiftRecord> previousMonthCalendar
    ) {

        if (hasAssignment(calendarDay, shiftType)) {
            return false;
        }

        for (User user : users) {

            ShiftPreference preference = getPreference(user, shiftType);

            if (preference == null || preference.getPriority() != priority) {
                continue;
            }

            ShiftRequest request = user.getShiftRequest();

            if (containsDay(request.getDatesNo(), calendarDay.getDate())) {
                continue;
            }

            boolean cap = shiftCountCapChecker(shiftCountCap, user);
            boolean weekendCap = weekendCapChecker(user, shiftType);
            boolean crossCheck = crossChecker(
                    calendarDay.getDate().getDayOfMonth(),
                    minimalGap,
                    user,
                    calendar,
                    calendar.getMonth().lengthOfMonth()
            );
            boolean previousMonthCheck = checkPreviousMonth(
                    previousMonthCalendar,
                    minimalGap,
                    calendarDay.getDate().getDayOfMonth(),
                    user
            );

            if (cap && !calendarDay.isWeekendOrHoliday() && crossCheck && previousMonthCheck) {
                incrementShiftCounter(user, shiftType);
                addAssignment(calendarDay, shiftType, user);
                return true;
            }

            if (cap && weekendCap && calendarDay.isWeekendOrHoliday() && crossCheck && previousMonthCheck) {
                incrementWeekendCounter(user, shiftType);
                addAssignment(calendarDay, shiftType, user);
                return true;
            }
        }

        return false;
    }

    private boolean tryAssignNormalShift(
            ScheduleCalendar calendar,
            CalendarDay calendarDay,
            List<User> users,
            int shiftType,
            int priority,
            int shiftCountCap,
            int minimalGap,
            Map<Integer, PreviousMonthShiftRecord> previousMonthCalendar
    ) {

        if (hasAssignment(calendarDay, shiftType)) {
            return false;
        }

        for (User user : users) {

            ShiftPreference preference = getPreference(user, shiftType);

            if (preference == null || preference.getPriority() != priority) {
                continue;
            }

            ShiftRequest request = user.getShiftRequest();

            boolean dateExplicitlySelected =
                    containsDay(preference.getDatesYes(), calendarDay.getDate());

            boolean anyDateSelected =
                    preference.isAnyDateSelected();

            boolean dateRejected =
                    containsDay(request.getDatesNo(), calendarDay.getDate());

            if ((!dateExplicitlySelected && !anyDateSelected) || dateRejected) {
                continue;
            }

            boolean cap = shiftCountCapChecker(shiftCountCap, user);
            boolean userCap = userShiftCountChecker(user, shiftType);
            boolean weekendCap = weekendCapChecker(user, shiftType);
            boolean crossCheck = crossChecker(
                    calendarDay.getDate().getDayOfMonth(),
                    minimalGap,
                    user,
                    calendar,
                    calendar.getMonth().lengthOfMonth()
            );
            boolean previousMonthCheck = checkPreviousMonth(
                    previousMonthCalendar,
                    minimalGap,
                    calendarDay.getDate().getDayOfMonth(),
                    user
            );

            if (!calendarDay.isWeekendOrHoliday()) {
                if (cap && userCap && crossCheck && previousMonthCheck) {
                    incrementShiftCounter(user, shiftType);
                    addAssignment(calendarDay, shiftType, user);
                    return true;
                }
            }

            if (calendarDay.isWeekendOrHoliday()) {
                if (cap && weekendCap && crossCheck && previousMonthCheck) {
                    incrementWeekendCounter(user, shiftType);
                    addAssignment(calendarDay, shiftType, user);
                    return true;
                }
            }
        }

        return false;
    }

    private List<Integer> getShiftTypes() {
        return IntStream.rangeClosed(1, shiftTypeProperties.count())
                .boxed()
                .toList();
    }

    private List<Integer> resolveShiftCalculationOrder(
            CalculationProfileForm form,
            List<Integer> shiftTypes
    ) {

        List<Integer> forceFillShiftTypes = form.getForceFillShiftTypes() == null
                ? List.of()
                : form.getForceFillShiftTypes();

        List<Integer> remainingShiftTypes = shiftTypes.stream()
                .filter(shiftType -> !forceFillShiftTypes.contains(shiftType))
                .toList();

        List<Integer> result = new ArrayList<>();
        result.addAll(forceFillShiftTypes);
        result.addAll(remainingShiftTypes);

        return result;
    }

    private List<User> findUsersWithRequest() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .filter(User::hasShiftRequest)
                .toList();
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

    private Map<Integer, List<User>> prepareUsersByShiftType(
            List<User> users,
            List<Integer> shiftTypes,
            boolean sortByDatesAmount
    ) {

        Map<Integer, List<User>> result = new HashMap<>();

        for (Integer shiftType : shiftTypes) {

            List<User> usersForSpecificDates = new ArrayList<>();
            List<User> usersForAnyDate = new ArrayList<>();

            for (User user : users) {

                if (!user.getAllowedShiftTypes().contains(shiftType)) {
                    continue;
                }

                ShiftPreference preference = getPreference(user, shiftType);

                if (preference == null || preference.isNoShiftRequested()) {
                    continue;
                }

                if (preference.isAnyDateSelected()) {
                    usersForAnyDate.add(user);
                } else {
                    usersForSpecificDates.add(user);
                }
            }

            if (sortByDatesAmount) {
                usersForSpecificDates.sort(
                        Comparator.comparingInt(user -> getPreference(user, shiftType).getDatesYes().size())
                );
            } else {
                Collections.shuffle(usersForSpecificDates);
            }

            Collections.shuffle(usersForAnyDate);

            List<User> combined = new ArrayList<>();
            combined.addAll(usersForSpecificDates);
            combined.addAll(usersForAnyDate);

            result.put(shiftType, combined);
        }

        return result;
    }

    private void reshuffleUsersByShiftType(
            Map<Integer, List<User>> usersByShiftType,
            boolean sortByDatesAmount
    ) {
        for (List<User> users : usersByShiftType.values()) {
            if (!sortByDatesAmount) {
                Collections.shuffle(users);
            }
        }
    }

    private ScheduleCalendar createEmptyScheduleCalendar(
            CalculationProfileForm form,
            YearMonth month,
            List<LocalDate> holidays
    ) {

        List<CalendarDay> days = IntStream.rangeClosed(1, month.lengthOfMonth())
                .mapToObj(month::atDay)
                .map(date -> CalendarDay.builder()
                        .date(date)
                        .weekendOrHoliday(isWeekendOrHoliday(date, holidays))
                        .assignments(new ArrayList<>())
                        .build())
                .toList();

        return ScheduleCalendar.builder()
                .month(month)
                .calculationProfile(form)
                .days(new ArrayList<>(days))
                .hitCounter(0)
                .overrideUserShiftRequestExceptNoDates(false)
                .build();
    }

    private CalendarDay getCalendarDay(ScheduleCalendar calendar, LocalDate date) {
        return calendar.getDays()
                .stream()
                .filter(day -> day.getDate().equals(date))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Calendar day not found: " + date));
    }

    private boolean isWeekendOrHoliday(LocalDate date, List<LocalDate> holidays) {
        return date.getDayOfWeek().getValue() >= 6 || holidays.contains(date);
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

    private void incrementShiftCounter(User user, int shiftType) {
        shiftCounters
                .computeIfAbsent(user.getId(), id -> new HashMap<>())
                .merge(shiftType, 1, Integer::sum);
    }

    private void incrementWeekendCounter(User user, int shiftType) {
        weekendCounters
                .computeIfAbsent(user.getId(), id -> new HashMap<>())
                .merge(shiftType, 1, Integer::sum);
    }

    private int getShiftCounter(User user, int shiftType) {
        return shiftCounters
                .getOrDefault(user.getId(), Map.of())
                .getOrDefault(shiftType, 0);
    }

    private int getWeekendCounter(User user, int shiftType) {
        return weekendCounters
                .getOrDefault(user.getId(), Map.of())
                .getOrDefault(shiftType, 0);
    }

    private void resetTemporaryCounters(List<User> users) {
        for (User user : users) {
            shiftCounters.remove(user.getId());
            weekendCounters.remove(user.getId());
        }
    }

    private boolean containsDay(List<LocalDate> dates, LocalDate targetDate) {
        if (dates == null || targetDate == null) {
            return false;
        }

        return dates.stream()
                .anyMatch(date -> date.getDayOfMonth() == targetDate.getDayOfMonth());
    }

    private Map<Integer, PreviousMonthShiftRecord> loadPreviousMonthCalendar(
            LocalDate adminDate,
            int minimalGap,
            List<Integer> shiftTypes
    ) {
        List<LocalDate> previousMonthDates =
                createPreviousMonthDatesToCheck(adminDate, minimalGap);

        Map<Integer, PreviousMonthShiftRecord> previousMonthCalendar = new HashMap<>();

        int backwardIndex = 0;

        for (LocalDate previousMonthDate : previousMonthDates) {

            Long dateId = toDateId(previousMonthDate);

            StoredCalendarDay storedCalendarDay =
                    storedCalendarDayRepository.findById(dateId).orElse(null);

            if (storedCalendarDay != null) {

                PreviousMonthShiftRecord previousMonthShiftRecord =
                        PreviousMonthShiftRecord.builder()
                                .backwardIndex(backwardIndex)
                                .dateStringDB(String.valueOf(dateId))
                                .dateIdDB(dateId.intValue())
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





    private boolean shiftCountCapChecker(Integer shiftCountCap, User user) {

        if (shiftCountCap == null) {
            return true;
        }

        int shiftCountTotal = getTotalShiftCount(user);

        if (shiftCountTotal == 0) {
            return true;
        }

        return shiftCountTotal < shiftCountCap;
    }

    private int getTotalShiftCount(User user) {
        Map<Integer, Integer> userShiftCounters =
                shiftCounters.getOrDefault(user.getId(), Map.of());

        Map<Integer, Integer> userWeekendCounters =
                weekendCounters.getOrDefault(user.getId(), Map.of());

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

    private boolean userShiftCountChecker(User user, int shiftType) {

        ShiftPreference preference = getPreference(user, shiftType);

        if (preference == null) {
            return false;
        }

        int requestedWeekdayCount = preference.getWeekdayCount();
        int currentWeekdayCount = getShiftCounter(user, shiftType);

        return requestedWeekdayCount != 0
                && currentWeekdayCount < requestedWeekdayCount;
    }

    private boolean weekendCapChecker(User user, int shiftType) {

        ShiftPreference preference = getPreference(user, shiftType);

        if (preference == null) {
            return false;
        }

        int requestedWeekendCount = preference.getWeekendCount();
        int currentWeekendCount = getWeekendCounter(user, shiftType);

        return requestedWeekendCount != 0
                && currentWeekendCount < requestedWeekendCount;
    }

    private boolean crossChecker(
            Integer calendarDay,
            int minimalGap,
            User user,
            ScheduleCalendar calendar,
            Integer monthSize
    ) {
        if (calendarDay == null || user == null || calendar == null || monthSize == null) {
            return true;
        }

        List<Integer> adjacentDatesList =
                createAdjacentDatesList(calendarDay, minimalGap, monthSize);

        for (Integer adjacentDay : adjacentDatesList) {

            boolean userAlreadyAssignedOnAdjacentDay =
                    calendar.getDays()
                            .stream()
                            .filter(day -> day.getDate().getDayOfMonth() == adjacentDay)
                            .flatMap(day -> day.getAssignments().stream())
                            .map(ShiftAssignment::getUser)
                            .filter(Objects::nonNull)
                            .anyMatch(assignedUser ->
                                    user.getUsername().equals(assignedUser.getUsername()));

            if (userAlreadyAssignedOnAdjacentDay) {
                return false;
            }
        }

        return true;
    }

    private boolean checkPreviousMonth(
            Map<Integer, PreviousMonthShiftRecord> previousMonthCalendarAll,
            Integer minimalGap,
            Integer date,
            User user
    ) {
        if (previousMonthCalendarAll == null || minimalGap == null || date == null || user == null) {
            return true;
        }

        int dateMinusMinimalGap = date - minimalGap;

        if (dateMinusMinimalGap <= 0) {
            for (int negativeDate = 0; negativeDate > -minimalGap; negativeDate--) {

                PreviousMonthShiftRecord previousMonthDay =
                        previousMonthCalendarAll.get(negativeDate);

                if (previousMonthDay != null
                        && previousMonthDay.containsUsername(user.getUsername())) {
                    return false;
                }
            }
        }

        return true;
    }

    private ScheduleCalendar countShifts(ScheduleCalendar calendar) {

        for (CalendarDay day : calendar.getDays()) {

            for (ShiftAssignment assignment : day.getAssignments()) {

                User user = assignment.getUser();

                if (user == null || user.getName() == null || user.getName().isBlank()) {
                    continue;
                }

                int shiftType = assignment.getShiftType();

                if (day.isWeekendOrHoliday()) {
                    incrementWeekendCounter(user, shiftType);
                } else {
                    incrementShiftCounter(user, shiftType);
                }
            }
        }

        return calendar;
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

    private static Long toDateId(LocalDate date) {
        return Long.valueOf(date.format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    private static List<Integer> createAdjacentDatesList(
            int calendarDay,
            int minimalGap,
            int monthSize
    ) {
        List<Integer> adjacentDates = new ArrayList<>();

        for (int day = calendarDay + 1; day <= calendarDay + minimalGap && day <= monthSize; day++) {
            adjacentDates.add(day);
        }

        for (int day = calendarDay - minimalGap; day < calendarDay; day++) {
            if (day > 0) {
                adjacentDates.add(day);
            }
        }

        adjacentDates.add(calendarDay);

        return adjacentDates;
    }

    private List<LocalDate> getHolidaysCzechRepublic(YearMonth calculationMonth) {

        int year = calculationMonth.getYear();

        return List.of(
                LocalDate.of(year, 1, 1),
                calculateGoodFriday(year),
                calculateEasterMonday(year),
                LocalDate.of(year, 5, 1),
                LocalDate.of(year, 5, 8),
                LocalDate.of(year, 7, 5),
                LocalDate.of(year, 7, 6),
                LocalDate.of(year, 9, 28),
                LocalDate.of(year, 10, 28),
                LocalDate.of(year, 11, 17),
                LocalDate.of(year, 12, 24),
                LocalDate.of(year, 12, 25),
                LocalDate.of(year, 12, 26)
        );
    }

    private static LocalDate calculateGoodFriday(int year) {
        int g = year % 19;
        int c = year / 100;
        int h = (c - (c / 4) - ((8 * c + 13) / 25) + 19 * g + 15) % 30;
        int i = h - (h / 28) * (1 - (h / 28) * (29 / (h + 1)) * ((21 - g) / 11));

        int day = (i - ((year + (year / 4) + i + 2 - c + (c / 4)) % 7) + 28) - 2;
        int month = 3;

        if (day > 31) {
            month++;
            day -= 31;
        }

        return LocalDate.of(year, month, day);
    }

    private static LocalDate calculateEasterMonday(int year){
        return calculateGoodFriday(year).plusDays(3);
    }

















}
