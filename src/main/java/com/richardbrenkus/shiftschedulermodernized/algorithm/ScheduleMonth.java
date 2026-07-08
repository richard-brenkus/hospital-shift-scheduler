package com.richardbrenkus.shiftschedulermodernized.algorithm;

import java.time.YearMonth;
import java.util.*;

import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleMonth {

    private YearMonth month;
    private int hitCounter;

    private List<ScheduleDay> days = new ArrayList<>();

    private CalculationProfileForm calculationProfile;

    private boolean overrideUserShiftRequestExceptNoDates;
    private boolean overrideUserShiftRequestAll;
    private boolean overrideShiftCountCap;
    private boolean overrideConflictingDates;
    private boolean overrideHasShiftRequest;
    private boolean overridePreviousMonthValid;
}
