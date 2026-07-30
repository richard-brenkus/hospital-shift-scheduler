package com.richardbrenkus.hospitalshiftscheduler.algorithm;

import java.time.YearMonth;
import java.util.*;

import com.richardbrenkus.hospitalshiftscheduler.dto.form.CalculationProfileForm;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleMonth {

    private YearMonth month;
    private int hitCounter;

    @Builder.Default
    private List<ScheduleDay> days = new ArrayList<>();

    private CalculationProfileForm calculationProfile;

    private boolean overrideUserShiftRequestExceptNoDates;
    private boolean overrideUserShiftRequestAll;
    private boolean overrideShiftCountCap;
    private boolean overrideConflictingDates;
    private boolean overrideHasShiftRequest;
    private boolean overridePreviousMonthValid;
}
