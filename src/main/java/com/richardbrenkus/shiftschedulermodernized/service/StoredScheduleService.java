package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalendarDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleCalendar;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredCalendarDay;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredCalendarDayRepository;
import com.richardbrenkus.shiftschedulermodernized.util.CalendarDateIdUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StoredScheduleService {

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER =
            DateTimeFormatter.ofPattern("MM/yyyy");

    private final StoredCalendarDayRepository storedCalendarDayRepository;

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

    private StoredCalendarDay toStoredCalendarDay(
            CalendarDay day,
            String monthYearId
    ) {
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
