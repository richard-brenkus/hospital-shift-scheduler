package com.richardbrenkus.shiftschedulermodernized.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@Transactional
public class CalendarService {

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
