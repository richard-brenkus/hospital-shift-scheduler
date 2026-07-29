package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleValidationResult;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.UserCalculationData;
import com.richardbrenkus.shiftschedulermodernized.dto.view.MonthOption;
import com.richardbrenkus.shiftschedulermodernized.dto.view.SavedScheduleDayView;
import com.richardbrenkus.shiftschedulermodernized.dto.view.SavedScheduleShiftAssignmentView;
import com.richardbrenkus.shiftschedulermodernized.dto.view.SavedScheduleView;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredUserSnapshot;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredScheduleDayRepository;
import com.richardbrenkus.shiftschedulermodernized.util.CalendarDateIdUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.*;

import static com.richardbrenkus.shiftschedulermodernized.config.constants.ApplicationConstants.*;

@Service
@RequiredArgsConstructor
public class StoredScheduleService {

    private final StoredScheduleDayRepository storedScheduleDayRepository;
    private final ShiftTypeService shiftTypeService;
    private final UserStatisticService userStatisticService;

    @Transactional
    public void saveScheduleWithStats(ScheduleMonth scheduleMonth, ScheduleValidationResult validationResult) {
        if (scheduleMonth == null || scheduleMonth.getMonth() == null) {
            throw new IllegalArgumentException("Cannot save schedule: schedule or schedule month is missing.");
        }

        if (scheduleMonth.getDays() == null || scheduleMonth.getDays().isEmpty()) {
            throw new IllegalArgumentException("Cannot save schedule: schedule contains no days.");
        }

        YearMonth month = scheduleMonth.getMonth();
        String monthYearId = month.format(MONTH_YEAR_FORMATTER);

        List<StoredScheduleDay> storedDays = scheduleMonth.getDays().stream().filter(Objects::nonNull).sorted(Comparator.comparing(ScheduleDay::getDate)).map(day -> toStoredScheduleDay(day, monthYearId)).toList();

        storedScheduleDayRepository.saveAll(storedDays);

        userStatisticService.replaceStatsForMonth(scheduleMonth.getMonth(), validationResult.getFullUserStatsByShiftType());

    }

    @Transactional(readOnly = true)
    public boolean existsByMonth(YearMonth month) {
        if (month == null) {
            return false;
        }

        String monthYearId = toMonthYearId(month);

        return !storedScheduleDayRepository.findByMonthYearIdOrderByDayIntegerAsc(monthYearId).isEmpty();
    }

    @Transactional(readOnly = true)
    public SavedScheduleView loadSavedScheduleView(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("Selected month must not be null.");
        }

        String monthYearId = toMonthYearId(month);

        List<StoredScheduleDay> storedDays = storedScheduleDayRepository.findByMonthYearIdOrderByDayIntegerAsc(monthYearId);

        List<Integer> shiftTypes = shiftTypeService.getShiftTypes();

        List<SavedScheduleDayView> dayViews = new ArrayList<>();

        for (int dayOfMonth = 1; dayOfMonth <= month.lengthOfMonth(); dayOfMonth++) {
            int currentDay = dayOfMonth;

            StoredScheduleDay storedDay = storedDays.stream().filter(day -> day.getDayInteger() != null && day.getDayInteger() == currentDay).findFirst().orElse(null);

            dayViews.add(toDayView(month, currentDay, storedDay, shiftTypes));
        }

        return SavedScheduleView.builder().month(month).shiftTypes(shiftTypes).days(dayViews).build();
    }

    @Transactional(readOnly = true)
    public List<MonthOption> getSelectableMonthOptions() {
        /*
         * +2 months, +1 month, current month, then previous 12 months.
         *
         * Replace this with getSavedMonthOptionsOnly()
         * to show only months that actually exist in DB.
         */
        YearMonth currentMonth = YearMonth.now();

        List<YearMonth> months = new ArrayList<>();
        months.add(currentMonth.plusMonths(2));
        months.add(currentMonth.plusMonths(1));
        months.add(currentMonth);

        for (long monthsBack = 1; monthsBack < 13; monthsBack++) {
            months.add(currentMonth.minusMonths(monthsBack));
        }

        return months.stream().map(this::toMonthOption).toList();
    }

    @Transactional
    public boolean existsByMonthYearId(String monthYearId) {
        return storedScheduleDayRepository.existsByMonthYearId(monthYearId);
    }

    private SavedScheduleDayView toDayView(YearMonth month, int dayOfMonth, StoredScheduleDay storedDay, List<Integer> shiftTypes) {
        List<SavedScheduleShiftAssignmentView> assignmentViews = new ArrayList<>();

        for (Integer shiftType : shiftTypes) {
            StoredUserSnapshot userSnapshot = storedDay == null ? null : storedDay.getAssignmentsByShiftType().get(shiftType);

            assignmentViews.add(SavedScheduleShiftAssignmentView.builder().shiftType(shiftType).user(userSnapshot).displayName(toDisplayName(userSnapshot)).build());
        }

        return SavedScheduleDayView.builder().date(month.atDay(dayOfMonth)).weekendOrHoliday(storedDay != null && storedDay.isWeekendOrHoliday()).assignments(assignmentViews).build();
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

    private StoredScheduleDay toStoredScheduleDay(ScheduleDay day, String monthYearId) {
        if (day.getDate() == null) {
            throw new IllegalArgumentException("Cannot save schedule: schedule date is missing.");
        }

        StoredScheduleDay storedDay = StoredScheduleDay.builder().dateId(CalendarDateIdUtils.toDateId(day.getDate())).monthYearId(monthYearId).weekendOrHoliday(day.isWeekendOrHoliday()).dayInteger(day.getDate().getDayOfMonth()).assignmentsByShiftType(new HashMap<>()).build();

        if (day.getAssignments() == null) {
            return storedDay;
        }

        for (ShiftAssignment assignment : day.getAssignments()) {
            if (assignment == null || assignment.getUserCalculationData() == null) {
                continue;
            }

            UserCalculationData userCalculationData = assignment.getUserCalculationData();

            if (userCalculationData.userId() == null) {
                continue;
            }

            storedDay.putAssignment(assignment.getShiftType(), userCalculationData.userId(), userCalculationData.username(), userCalculationData.name(), userCalculationData.title());
        }

        return storedDay;
    }
}
