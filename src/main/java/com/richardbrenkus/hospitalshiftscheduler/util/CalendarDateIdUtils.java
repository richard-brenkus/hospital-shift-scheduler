package com.richardbrenkus.hospitalshiftscheduler.util;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;

@Slf4j
public final class CalendarDateIdUtils {

    private CalendarDateIdUtils() {
    }

    public static Long toDateId(LocalDate date) {
        return Long.valueOf(date.format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    public static LocalDateTime returnAdjustedFinalSubmissionDateTime(int day) {

        LocalDateTime now = LocalDateTime.now();

        if (!dayExistsInMonth(day, now) || !isDayInFuture(day, now)) {
            return getFiveDaysBeforeEndOfCurrentMonth();
        }

        return LocalDateTime.of(now.getYear(), now.getMonth(), day, 0, 0);
    }

    public static final DateTimeFormatter CSV_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

    private static boolean dayExistsInMonth(int day, LocalDateTime now) {

        YearMonth currentMonth = YearMonth.from(now);
        int maximumDay = currentMonth.lengthOfMonth();

        return day <= maximumDay && day > 0;
    }

    private static boolean isDayInFuture(int day, LocalDateTime now) {
        return day > now.getDayOfMonth();
    }

    private static LocalDateTime getFiveDaysBeforeEndOfCurrentMonth() {
        return LocalDateTime.of(YearMonth.now().atEndOfMonth().minusDays(5), LocalTime.MAX);
    }

}
