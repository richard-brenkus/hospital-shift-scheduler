package com.richardbrenkus.shiftschedulermodernized.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class CalendarDateIdUtils {

    private CalendarDateIdUtils() {}

    public static Long toDateId(LocalDate date) {
        return Long.valueOf(date.format(DateTimeFormatter.BASIC_ISO_DATE));
    }
}
