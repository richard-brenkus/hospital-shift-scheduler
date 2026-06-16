package com.richardbrenkus.shiftschedulermodernized.dto.form;

import java.util.HashSet;
import java.util.Set;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculationProfileForm {

    private int shiftCountCap;
    private int gapBetweenShifts;
    private boolean sortByDatesAmount;

    private int year;
    private int month;

    private Set<Integer> forceFillShiftTypes = new HashSet<>();
}
