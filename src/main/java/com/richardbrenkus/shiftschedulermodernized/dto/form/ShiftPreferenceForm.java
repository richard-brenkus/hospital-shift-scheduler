package com.richardbrenkus.shiftschedulermodernized.dto.form;

import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftPreferenceForm {
    private int shiftType;
    private boolean noShiftRequested;
    private boolean anyDateSelected;
    private boolean datesYesUpdate;
    private List<LocalDate> datesYes = new ArrayList<>();
    private int priority;
    private int weekdayCount;
    private int weekendCount;
}
