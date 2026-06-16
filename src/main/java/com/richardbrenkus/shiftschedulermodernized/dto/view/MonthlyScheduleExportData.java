package com.richardbrenkus.shiftschedulermodernized.dto.view;

import com.richardbrenkus.shiftschedulermodernized.entity.ScheduleEntry;

import java.util.List;

public record MonthlyScheduleExportData(String monthString,
                                        String yearString,
                                        List<ScheduleEntry> scheduleEntries) {
}
