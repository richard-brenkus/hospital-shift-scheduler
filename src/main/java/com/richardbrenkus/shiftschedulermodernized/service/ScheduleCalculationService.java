package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.*;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredCalendarDay;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ScheduleCalculationService {

    private static final int NUMBER_OF_ATTEMPTS = 100;

    private final UserRepository userRepository;
    private final ShiftTypeService shiftTypeService;
    private final ScheduleRuleService scheduleRuleService;

    @Transactional(readOnly = true)
    public ScheduleCalendar calculateSchedule(CalculationProfileForm form) {

        boolean sortByDatesAmount = form.isSortByDatesAmount();

        int minimalGap = form.getGapBetweenShifts();
        int shiftCountCap = form.getShiftCountCap();

        YearMonth calculationMonth = form.getCalculationMonth();

        List<Integer> shiftTypes = shiftTypeService.getShiftTypes();
        List<Integer> calculationOrder = resolveShiftCalculationOrder(form, shiftTypes);
        List<Integer> priorities = IntStream.rangeClosed(1, 10)
                .boxed()
                .toList();

        List<User> usersWithRequest = findUsersWithRequest();

        LocalDate firstDayOfMonth = calculationMonth.atDay(1);
        List<LocalDate> holidaysCzech = getHolidaysCzechRepublic(form.getCalculationMonth());

        Map<Integer, StoredCalendarDay> previousMonthCalendar = scheduleRuleService.loadPreviousMonthCalendar(firstDayOfMonth, minimalGap);

        List<ScheduleCalendar> candidateCalendars = new ArrayList<>();

        for (int attempt = 0; attempt < NUMBER_OF_ATTEMPTS; attempt++) {

            CalculationCounters counters = new CalculationCounters();

            ScheduleCalendar calendar = createEmptyScheduleCalendar(form, calculationMonth, holidaysCzech);

            List<Integer> monthDays = IntStream.rangeClosed(1, calculationMonth.lengthOfMonth())
                    .boxed()
                    .collect(Collectors.toCollection(ArrayList::new));

            Collections.shuffle(monthDays);

            Map<Integer, UsersForShiftType> usersByShiftType = prepareUsersByShiftType(usersWithRequest, shiftTypes);

            int hitCounter = 0;

            for (Integer dayOfMonth : monthDays) {

                LocalDate date = calculationMonth.atDay(dayOfMonth);
                CalendarDay calendarDay = getCalendarDay(calendar, date);

                for (Integer priority : priorities) {

                    for (Integer shiftType : calculationOrder) {

                        List<User> orderedUsers = getUsersInCalculationOrder(usersByShiftType.get(shiftType), shiftType, sortByDatesAmount);

                        boolean forceFill = form.getForceFillShiftTypes() != null && form.getForceFillShiftTypes().contains(shiftType);

                        if (forceFill) {
                            boolean assigned = tryAssignForceFillShift(calendar, calendarDay, orderedUsers, shiftType, priority, shiftCountCap, minimalGap, previousMonthCalendar, counters);

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

                for (Integer priority : priorities) {

                    for (Integer shiftType : calculationOrder) {

                        List<User> orderedUsers = getUsersInCalculationOrder(usersByShiftType.get(shiftType), shiftType, sortByDatesAmount);

                        boolean forceFill = form.getForceFillShiftTypes() != null && form.getForceFillShiftTypes().contains(shiftType);

                        if (!forceFill) {
                            boolean assigned = tryAssignNormalShift(calendar, calendarDay, orderedUsers, shiftType, priority, shiftCountCap, minimalGap, previousMonthCalendar, counters);

                            if (assigned) {
                                hitCounter++;
                            }
                        }
                    }
                }
            }

            calendar.setHitCounter(hitCounter);
            candidateCalendars.add(calendar);
        }

        return candidateCalendars.stream()
                .max(Comparator.comparingInt(ScheduleCalendar::getHitCounter))
                .orElseThrow(() -> new IllegalStateException("No schedule calendar was created"));
    }

    private boolean tryAssignForceFillShift(ScheduleCalendar calendar, CalendarDay calendarDay, List<User> users, int shiftType, int priority, int shiftCountCap, int minimalGap, Map<Integer, StoredCalendarDay> previousMonthCalendar, CalculationCounters counters) {

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

            boolean withinTotalShiftLimit = scheduleRuleService.isWithinTotalShiftLimit(shiftCountCap, user, counters);
            boolean withinRequestedWeekendLimit = scheduleRuleService.isWithinRequestedWeekendLimit(user, shiftType, counters);
            boolean respectsMinimalGap = scheduleRuleService.respectsMinimalGap(calendarDay.getDate(), minimalGap, user, calendar, shiftType);
            boolean respectsPreviousMonthGap = scheduleRuleService.respectsPreviousMonthGap(previousMonthCalendar, minimalGap, calendarDay.getDate(), user);

            if (withinTotalShiftLimit && !calendarDay.isWeekendOrHoliday() && respectsMinimalGap && respectsPreviousMonthGap) {
                incrementShiftCounter(user, shiftType, counters);
                addAssignment(calendarDay, shiftType, user);
                return true;
            }

            if (withinTotalShiftLimit && withinRequestedWeekendLimit && calendarDay.isWeekendOrHoliday() && respectsMinimalGap && respectsPreviousMonthGap) {
                incrementWeekendCounter(user, shiftType, counters);
                addAssignment(calendarDay, shiftType, user);
                return true;
            }
        }

        return false;
    }

    private boolean tryAssignNormalShift(ScheduleCalendar calendar, CalendarDay calendarDay, List<User> users, int shiftType, int priority, int shiftCountCap, int minimalGap, Map<Integer, StoredCalendarDay> previousMonthCalendar, CalculationCounters counters) {

        if (hasAssignment(calendarDay, shiftType)) {
            return false;
        }

        for (User user : users) {

            ShiftPreference preference = getPreference(user, shiftType);

            if (preference == null || preference.getPriority() != priority) {
                continue;
            }

            ShiftRequest request = user.getShiftRequest();

            boolean dateExplicitlySelected = containsDay(preference.getDatesYes(), calendarDay.getDate());

            boolean anyDateSelected = preference.isAnyDateSelected();

            boolean dateRejected = containsDay(request.getDatesNo(), calendarDay.getDate());

            if ((!dateExplicitlySelected && !anyDateSelected) || dateRejected) {
                continue;
            }

            boolean withinTotalShiftLimit = scheduleRuleService.isWithinTotalShiftLimit(shiftCountCap, user, counters);
            boolean withinRequestedWeekdayLimit = scheduleRuleService.isWithinRequestedWeekdayLimit(user, shiftType, counters);
            boolean withinRequestedWeekendLimit = scheduleRuleService.isWithinRequestedWeekendLimit(user, shiftType, counters);
            boolean respectsMinimalGap = scheduleRuleService.respectsMinimalGap(calendarDay.getDate(), minimalGap, user, calendar, shiftType);
            boolean respectsPreviousMonthGap = scheduleRuleService.respectsPreviousMonthGap(previousMonthCalendar, minimalGap, calendarDay.getDate(), user);

            if (!calendarDay.isWeekendOrHoliday()) {
                if (withinTotalShiftLimit && withinRequestedWeekdayLimit && respectsMinimalGap && respectsPreviousMonthGap) {
                    incrementShiftCounter(user, shiftType, counters);
                    addAssignment(calendarDay, shiftType, user);
                    return true;
                }
            }

            if (calendarDay.isWeekendOrHoliday()) {
                if (withinTotalShiftLimit && withinRequestedWeekendLimit && respectsMinimalGap && respectsPreviousMonthGap) {
                    incrementWeekendCounter(user, shiftType, counters);
                    addAssignment(calendarDay, shiftType, user);
                    return true;
                }
            }
        }

        return false;
    }

    private List<Integer> resolveShiftCalculationOrder(CalculationProfileForm form, List<Integer> shiftTypes) {

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

    private Map<Integer, UsersForShiftType> prepareUsersByShiftType(List<User> users, List<Integer> shiftTypes) {

        Map<Integer, UsersForShiftType> result = new HashMap<>();

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

            result.put(shiftType, UsersForShiftType.builder()
                    .specificDateUsers(usersForSpecificDates)
                    .anyDateUsers(usersForAnyDate)
                    .build()
            );
        }

        return result;
    }


    private ScheduleCalendar createEmptyScheduleCalendar(CalculationProfileForm form, YearMonth month, List<LocalDate> holidays) {

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

    private void incrementShiftCounter(User user, int shiftType, CalculationCounters counters) {
        counters.getWeekdayCounters()
                .computeIfAbsent(user.getId(), id -> new HashMap<>())
                .merge(shiftType, 1, Integer::sum);
    }

    private void incrementWeekendCounter(User user, int shiftType, CalculationCounters counters) {
        counters.getWeekendCounters()
                .computeIfAbsent(user.getId(), id -> new HashMap<>())
                .merge(shiftType, 1, Integer::sum);
    }

    private boolean containsDay(List<LocalDate> dates, LocalDate targetDate) {
        if (dates == null || targetDate == null) {
            return false;
        }

        return dates.contains(targetDate);
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

    private static LocalDate calculateEasterMonday(int year) {
        return calculateGoodFriday(year).plusDays(3);
    }

    private List<User> getUsersInCalculationOrder(UsersForShiftType users, int shiftType, boolean sortByDatesAmount) {
        if (users == null) {
            return List.of();
        }

        List<User> specificDateUsers = new ArrayList<>(users.specificDateUsers() == null ? List.of() : users.specificDateUsers());

        List<User> anyDateUsers = new ArrayList<>(users.anyDateUsers() == null ? List.of() : users.anyDateUsers());

        if (sortByDatesAmount) {
            specificDateUsers.sort(
                    Comparator.comparingInt(user -> {
                        ShiftPreference preference = getPreference(user, shiftType);
                        return preference == null || preference.getDatesYes() == null
                                ? Integer.MAX_VALUE
                                : preference.getDatesYes().size();
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


}
