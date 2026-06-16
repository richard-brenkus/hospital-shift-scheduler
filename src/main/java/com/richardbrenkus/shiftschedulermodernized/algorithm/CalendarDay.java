package com.richardbrenkus.shiftschedulermodernized.algorithm;

import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarDay {

    private LocalDate date;
    private boolean weekendOrHoliday;

    private List<ShiftAssignment> assignments = new ArrayList<>();
}
