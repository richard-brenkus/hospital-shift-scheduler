package com.richardbrenkus.shiftschedulermodernized.algorithm.record;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculatedScheduleMonth {
    private YearMonth month;
    private int hitCounter;

    @Builder.Default
    private List<CalculatedScheduleDay> days = new ArrayList<>();
}
