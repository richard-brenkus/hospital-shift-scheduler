package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.dto.view.MonthlyScheduleExportData;
import com.richardbrenkus.shiftschedulermodernized.entity.MonthlySchedule;
import com.richardbrenkus.shiftschedulermodernized.entity.ScheduleEntry;
import com.richardbrenkus.shiftschedulermodernized.repository.MonthlyScheduleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class MonthlyScheduleService {

    private final MonthlyScheduleRepository repo;

    public MonthlyScheduleService(MonthlyScheduleRepository repo) {
        this.repo = repo;
    }

    public MonthlyScheduleExportData getCalendarExportData(int year, int month) {
        String monthString = month < 10 ? "0" + month : String.valueOf(month);
        String yearString = String.valueOf(year);
        String monthYearId = monthString + "/" + yearString;

        MonthlySchedule monthlySchedules = repo.getMonthlyScheduleByDateMonthYearId(monthYearId);

        return new MonthlyScheduleExportData(monthString, yearString, monthlySchedules.getAssignments());
    }

    public int returnVerifiedDay(int day, LocalDate localDate, ZoneId zoneId) {

        int resultingDay = day;

        if (localDate.isLeapYear()) {
            if (day > ZonedDateTime.now(zoneId).getMonth().maxLength())
                resultingDay = ZonedDateTime.now(zoneId).getMonth().maxLength();
        }
        if (!localDate.isLeapYear()) {
            if (day > ZonedDateTime.now(zoneId).getMonth().minLength())
                resultingDay = ZonedDateTime.now(zoneId).getMonth().minLength();
        }

        return resultingDay;
    }
}
