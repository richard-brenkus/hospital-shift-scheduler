package com.richardbrenkus.hospitalshiftscheduler.algorithm;

import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDay {

    private LocalDate date;
    private boolean weekendOrHoliday;

    @Builder.Default
    private List<ShiftAssignment> assignments = new ArrayList<>();
}
