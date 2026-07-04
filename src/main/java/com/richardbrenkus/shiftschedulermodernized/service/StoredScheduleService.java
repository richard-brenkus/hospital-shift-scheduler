package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalendarDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleCalendar;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.dto.view.MonthOption;
import com.richardbrenkus.shiftschedulermodernized.dto.view.SavedScheduleDayView;
import com.richardbrenkus.shiftschedulermodernized.dto.view.SavedScheduleShiftAssignmentView;
import com.richardbrenkus.shiftschedulermodernized.dto.view.SavedScheduleView;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredCalendarDay;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredUserSnapshot;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredCalendarDayRepository;
import com.richardbrenkus.shiftschedulermodernized.util.CalendarDateIdUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StoredScheduleService {

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter MONTH_YEAR_ID_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    private final StoredCalendarDayRepository storedCalendarDayRepository;
    private final ShiftTypeService shiftTypeService;

    @Transactional
    public void saveSchedule(ScheduleCalendar calendar) {
        if (calendar == null || calendar.getMonth() == null) {
            throw new IllegalArgumentException("Cannot save schedule: calendar or calendar month is missing.");
        }

        if (calendar.getDays() == null || calendar.getDays().isEmpty()) {
            throw new IllegalArgumentException("Cannot save schedule: calendar contains no days.");
        }

        YearMonth month = calendar.getMonth();
        String monthYearId = month.format(MONTH_YEAR_FORMATTER);

        List<StoredCalendarDay> storedDays = calendar.getDays()
                .stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CalendarDay::getDate))
                .map(day -> toStoredCalendarDay(day, monthYearId))
                .toList();

        storedCalendarDayRepository.saveAll(storedDays);
    }

    @Transactional(readOnly = true)
    public boolean existsByMonth(YearMonth month) {
        if (month == null) {
            return false;
        }

        String monthYearId = toMonthYearId(month);

        return !storedCalendarDayRepository
                .findByMonthYearIdOrderByDayIntegerAsc(monthYearId)
                .isEmpty();
    }

    @Transactional(readOnly = true)
    public SavedScheduleView loadSavedScheduleView(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("Selected month must not be null.");
        }

        String monthYearId = toMonthYearId(month);

        List<StoredCalendarDay> storedDays = storedCalendarDayRepository.findByMonthYearIdOrderByDayIntegerAsc(monthYearId);

        List<Integer> shiftTypes = shiftTypeService.getShiftTypes();

        List<SavedScheduleDayView> dayViews = new ArrayList<>();

        for (int dayOfMonth = 1; dayOfMonth <= month.lengthOfMonth(); dayOfMonth++) {
            int currentDay = dayOfMonth;

            StoredCalendarDay storedDay = storedDays.stream()
                    .filter(day -> day.getDayInteger() != null && day.getDayInteger() == currentDay)
                    .findFirst()
                    .orElse(null);

            dayViews.add(toDayView(month, currentDay, storedDay, shiftTypes));
        }

        return SavedScheduleView.builder()
                .month(month)
                .shiftTypes(shiftTypes)
                .days(dayViews)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MonthOption> getSelectableMonthOptions() {
        /*
         * +2 months, +1 month, current month, then previous 12 months.
         *
         * Replace this with getSavedMonthOptionsOnly()
         * if you want to show only months that actually exist in DB.
         */
        YearMonth currentMonth = YearMonth.now();

        List<YearMonth> months = new ArrayList<>();
        months.add(currentMonth.plusMonths(2));
        months.add(currentMonth.plusMonths(1));
        months.add(currentMonth);

        for (long monthsBack = 1; monthsBack < 13; monthsBack++) {
            months.add(currentMonth.minusMonths(monthsBack));
        }

        return months.stream()
                .map(this::toMonthOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonthOption> getSavedMonthOptionsOnly() {
        return storedCalendarDayRepository.findDistinctMonthYearIds()
                .stream()
                .map(this::parseMonthYearId)
                .sorted(Comparator.reverseOrder())
                .map(this::toMonthOption)
                .toList();
    }

    private SavedScheduleDayView toDayView(YearMonth month, int dayOfMonth, StoredCalendarDay storedDay, List<Integer> shiftTypes) {
        List<SavedScheduleShiftAssignmentView> assignmentViews = new ArrayList<>();

        for (Integer shiftType : shiftTypes) {
            StoredUserSnapshot userSnapshot = storedDay == null
                    ? null
                    : storedDay.getAssignmentsByShiftType().get(shiftType);

            assignmentViews.add(SavedScheduleShiftAssignmentView.builder()
                    .shiftType(shiftType)
                    .user(userSnapshot)
                    .displayName(toDisplayName(userSnapshot))
                    .build()
            );
        }

        return SavedScheduleDayView.builder()
                .date(month.atDay(dayOfMonth))
                .weekendOrHoliday(storedDay != null && storedDay.isWeekendOrHoliday())
                .assignments(assignmentViews)
                .build();
    }

    private String toDisplayName(StoredUserSnapshot userSnapshot) {
        if (userSnapshot == null) {
            return "";
        }

        String name = userSnapshot.getName() == null ? "" : userSnapshot.getName();

        if (userSnapshot.getTitle() == null || userSnapshot.getTitle().isBlank()) {
            return name;
        }

        return userSnapshot.getTitle() + " " + name;
    }

    private MonthOption toMonthOption(YearMonth month) {
        return new MonthOption(month, month.format(MONTH_LABEL_FORMATTER));
    }

    private String toMonthYearId(YearMonth month) {
        return month.format(MONTH_YEAR_ID_FORMATTER);
    }

    private YearMonth parseMonthYearId(String monthYearId) {
        return YearMonth.parse(monthYearId, MONTH_YEAR_ID_FORMATTER);
    }

    private StoredCalendarDay toStoredCalendarDay(CalendarDay day, String monthYearId) {
        if (day.getDate() == null) {
            throw new IllegalArgumentException("Cannot save schedule: calendar day date is missing.");
        }

        StoredCalendarDay storedDay = StoredCalendarDay.builder()
                .dateId(CalendarDateIdUtils.toDateId(day.getDate()))
                .monthYearId(monthYearId)
                .weekendOrHoliday(day.isWeekendOrHoliday())
                .dayInteger(day.getDate().getDayOfMonth())
                .assignmentsByShiftType(new HashMap<>())
                .build();

        if (day.getAssignments() == null) {
            return storedDay;
        }

        for (ShiftAssignment assignment : day.getAssignments()) {
            if (assignment == null || assignment.getUser() == null) {
                continue;
            }

            User user = assignment.getUser();

            if (user.getId() == null) {
                continue;
            }

            storedDay.putAssignment(
                    assignment.getShiftType(),
                    user.getId(),
                    user.getUsername(),
                    user.getName(),
                    user.getTitle()
            );
        }

        return storedDay;
    }
}
