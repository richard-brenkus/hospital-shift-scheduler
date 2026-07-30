package com.richardbrenkus.hospitalshiftscheduler.dto.form;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculationProfileForm {

    private int shiftCountCap;
    private int gapBetweenShifts;
    private boolean sortByDatesAmount;

    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth calculationMonth;

    @Builder.Default
    private List<Integer> forceFillShiftTypes = new ArrayList<>();
}
