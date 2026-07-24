package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.algorithm.record.UserCalculationData;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftAssignment {

    private int shiftType;
    private UserCalculationData userCalculationData;
}
