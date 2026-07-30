package com.richardbrenkus.hospitalshiftscheduler.dto.form;

import lombok.*;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleEditForm {

    private YearMonth month;
    private int shiftCountCap;
    private int gapBetweenShifts;
    private boolean sortByDatesAmount;

    @Builder.Default
    private List<Integer> forceFillShiftTypes = new ArrayList<>();

    @Builder.Default
    private List<ScheduleDayForm> days = new ArrayList<>();

    private boolean overrideUserShiftRequestExceptNoDates;
    private boolean overrideUserShiftRequestAll;
    private boolean overrideShiftCountCap;
    private boolean overrideConflictingDates;
    private boolean overrideHasShiftRequest;
    private boolean overridePreviousMonthValid;

    public CalculationProfileForm toCalculationProfileForm() {
        return CalculationProfileForm.builder()
                .calculationMonth(month)
                .shiftCountCap(shiftCountCap)
                .gapBetweenShifts(gapBetweenShifts)
                .sortByDatesAmount(sortByDatesAmount)
                .forceFillShiftTypes(forceFillShiftTypes)
                .build();
    }
}
