package com.richardbrenkus.hospitalshiftscheduler.algorithm.record;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculatedScheduleDay {
    private LocalDate date;
    private boolean weekendOrHoliday;

    @Builder.Default
    private List<CalculatedShiftAssignment> assignments = new ArrayList<>();

}
