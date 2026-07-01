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

import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class StoredScheduleService {

    private final StoredCalendarDayRepository storedCalendarDayRepository;

    @Transactional
    public void saveSchedule(ScheduleCalendar calendar) {

        if (calendar == null || calendar.getMonth() == null) {
            throw new IllegalArgumentException("Schedule calendar or month is missing");
        }

        String monthYearId = calendar.getMonth()
                .format(DateTimeFormatter.ofPattern("MM/yyyy"));

        for (CalendarDay day : calendar.getDays()) {

            StoredCalendarDay storedDay = StoredCalendarDay.builder()
                    .dateId(CalendarDateIdUtils.toDateId(day.getDate()))
                    .monthYearId(monthYearId)
                    .dayInteger(day.getDate().getDayOfMonth())
                    .weekendOrHoliday(day.isWeekendOrHoliday())
                    .assignmentsByShiftType(new HashMap<>())
                    .build();

            for (ShiftAssignment assignment : day.getAssignments()) {
                User user = assignment.getUser();

                if (user == null || user.getName() == null || user.getName().isBlank()) {
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

            storedCalendarDayRepository.save(storedDay);
        }
    }
}
