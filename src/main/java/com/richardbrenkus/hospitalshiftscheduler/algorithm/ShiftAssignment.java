package com.richardbrenkus.hospitalshiftscheduler.algorithm;

import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.UserCalculationData;
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
