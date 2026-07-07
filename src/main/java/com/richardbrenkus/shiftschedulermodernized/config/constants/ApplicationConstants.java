package com.richardbrenkus.shiftschedulermodernized.config.constants;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ApplicationConstants {

    private ApplicationConstants() {
    }

    public static final ZoneId ZONE_ID = ZoneId.of("Europe/Prague");

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

}
