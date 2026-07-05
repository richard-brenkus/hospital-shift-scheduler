package com.richardbrenkus.shiftschedulermodernized.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@Transactional
public class CalendarService {

	/*private final CalendarRepository repo;

	public CalendarService(CalendarRepository repo) {
		this.repo = repo;
	}

	public List<CalendarObjectDB> getCalendarByMonthYearId(String monthYearId){
		return new ArrayList<>(repo.getCalendarByDateMonthYearId(monthYearId));
	}

	public CalendarExportData getCalendarExportData(int year, int month) {
		String monthString = month < 10 ? "0" + month : String.valueOf(month);
		String yearString = String.valueOf(year);
		String monthYearId = monthString + "/" + yearString;

		List<CalendarObjectDB> calendarObjects = this.getCalendarByMonthYearId(monthYearId);

		calendarObjects.sort(Comparator.comparing(CalendarObjectDB::getDayInteger));

		return new CalendarExportData(monthString, yearString, calendarObjects);
	}*/

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
