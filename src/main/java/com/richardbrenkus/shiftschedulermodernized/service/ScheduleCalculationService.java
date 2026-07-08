package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.UsersForShiftType;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final ScheduleGenerationEngine scheduleGenerationEngine;

    @Transactional(readOnly = true)
    public ScheduleMonth calculateSchedule(CalculationProfileForm form) {

        YearMonth calculationMonth = form.getCalculationMonth();
        int minimalGap = form.getGapBetweenShifts();

        List<Integer> shiftTypes = shiftTypeService.getShiftTypes();
        List<Integer> calculationOrder = resolveShiftCalculationOrder(form, shiftTypes);
        List<Integer> priorities = IntStream.rangeClosed(1, 10)
                .boxed()
                .toList();

        List<User> usersWithRequest = findUsersWithRequest();

        Map<Integer, UsersForShiftType> usersByShiftType = prepareUsersByShiftType(
                usersWithRequest,
                shiftTypes,
                calculationMonth
        );

        List<LocalDate> holidaysCzech = getHolidaysCzechRepublic(calculationMonth);
        Map<Integer, StoredScheduleDay> storedScheduleDayMap =
                scheduleRuleService.loadPreviousStoredScheduleDays(calculationMonth.atDay(1), minimalGap);

        List<ScheduleMonth> candidateSchedules = new ArrayList<>();

        for (int attempt = 0; attempt < NUMBER_OF_ATTEMPTS; attempt++) {
            ScheduleMonth scheduleMonth = createCandidateScheduleMonth(
                    form,
                    calculationMonth,
                    holidaysCzech,
                    calculationOrder,
                    priorities,
                    usersByShiftType,
                    storedScheduleDayMap
            );

            candidateSchedules.add(scheduleMonth);
        }

        return selectBestScheduleMonth(candidateSchedules);
    }

    private ScheduleMonth createCandidateScheduleMonth(
            CalculationProfileForm form,
            YearMonth calculationMonth,
            List<LocalDate> holidaysCzech,
            List<Integer> calculationOrder,
            List<Integer> priorities,
            Map<Integer, UsersForShiftType> usersByShiftType,
            Map<Integer, StoredScheduleDay> previousMonthStoredScheduleDays
    ) {
        CalculationCounters counters = new CalculationCounters();
        ScheduleMonth scheduleMonth = createEmptyScheduleMonth(form, calculationMonth, holidaysCzech);
        List<Integer> monthDays = createShuffledMonthDays(calculationMonth);

        List<Integer> forceFillShiftTypes = form.getForceFillShiftTypes() == null
                ? List.of()
                : form.getForceFillShiftTypes();

        int hitCounter = 0;

        hitCounter += scheduleGenerationEngine.assignForceFillShifts(
                scheduleMonth,
                monthDays,
                priorities,
                calculationOrder,
                usersByShiftType,
                forceFillShiftTypes,
                form.isSortByDatesAmount(),
                form.getShiftCountCap(),
                form.getGapBetweenShifts(),
                previousMonthStoredScheduleDays,
                counters
        );

        hitCounter += scheduleGenerationEngine.assignRegularShifts(
                scheduleMonth,
                monthDays,
                priorities,
                calculationOrder,
                usersByShiftType,
                forceFillShiftTypes,
                form.isSortByDatesAmount(),
                form.getShiftCountCap(),
                form.getGapBetweenShifts(),
                previousMonthStoredScheduleDays,
                counters
        );

        scheduleMonth.setHitCounter(hitCounter);
        return scheduleMonth;
    }

    private ScheduleMonth selectBestScheduleMonth(List<ScheduleMonth> candidateScheduleMonths) {
        return candidateScheduleMonths.stream()
                .max(Comparator.comparingInt(ScheduleMonth::getHitCounter))
                .orElseThrow(() -> new IllegalStateException("No schedule was created"));
    }

    private List<Integer> createShuffledMonthDays(YearMonth calculationMonth) {
        List<Integer> monthDays = IntStream.rangeClosed(1, calculationMonth.lengthOfMonth())
                .boxed()
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.shuffle(monthDays);
        return monthDays;
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

    private Map<Integer, UsersForShiftType> prepareUsersByShiftType(
            List<User> users,
            List<Integer> shiftTypes,
            YearMonth calculationMonth
    ) {
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
                    continue;
                }

                if (hasRequestedDateInMonth(preference, calculationMonth)) {
                    usersForSpecificDates.add(user);
                }
            }

            result.put(
                    shiftType,
                    UsersForShiftType.builder()
                            .specificDateUsers(usersForSpecificDates)
                            .anyDateUsers(usersForAnyDate)
                            .build()
            );
        }

        return result;
    }

    private boolean hasRequestedDateInMonth(ShiftPreference preference, YearMonth calculationMonth) {
        if (preference == null || preference.getDatesYes() == null || calculationMonth == null) {
            return false;
        }

        return preference.getDatesYes()
                .stream()
                .anyMatch(date -> date != null && YearMonth.from(date).equals(calculationMonth));
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

    private ScheduleMonth createEmptyScheduleMonth(
            CalculationProfileForm form,
            YearMonth month,
            List<LocalDate> holidays
    ) {
        List<ScheduleDay> days = IntStream.rangeClosed(1, month.lengthOfMonth())
                .mapToObj(month::atDay)
                .map(date -> ScheduleDay.builder()
                        .date(date)
                        .weekendOrHoliday(isWeekendOrHoliday(date, holidays))
                        .assignments(new ArrayList<>())
                        .build())
                .toList();

        return ScheduleMonth.builder()
                .month(month)
                .calculationProfile(form)
                .days(new ArrayList<>(days))
                .hitCounter(0)
                .overrideUserShiftRequestExceptNoDates(false)
                .build();
    }

    private boolean isWeekendOrHoliday(LocalDate date, List<LocalDate> holidays) {
        return date.getDayOfWeek().getValue() >= 6 || holidays.contains(date);
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
}
