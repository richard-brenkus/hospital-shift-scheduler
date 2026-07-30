package com.richardbrenkus.hospitalshiftscheduler.dto.view;

import lombok.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedScheduleView {

    private YearMonth month;

    @Builder.Default
    private List<Integer> shiftTypes = new ArrayList<>();

    @Builder.Default
    private List<SavedScheduleDayView> days = new ArrayList<>();

    public List<Integer> monthDaysList() {
        if (days == null) {
            return List.of();
        }

        return days.stream()
                .map(SavedScheduleDayView::getDate)
                .filter(Objects::nonNull)
                .map(LocalDate::getDayOfMonth)
                .toList();
    }

    public List<Integer> weekendsAndHolidays() {
        if (days == null) {
            return List.of();
        }

        return days.stream()
                .filter(SavedScheduleDayView::isWeekendOrHoliday)
                .map(SavedScheduleDayView::getDate)
                .filter(Objects::nonNull)
                .map(LocalDate::getDayOfMonth)
                .toList();
    }
}

