package com.richardbrenkus.shiftschedulermodernized.dto.form;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ShiftRequestForm {
    private List<LocalDate> datesNo = new ArrayList<>();
    private List<ShiftPreferenceForm> preferences = new ArrayList<>();
    private boolean datesNoUpdate;
    private int shiftCount;
    private int weekendCount;
    private int priority;

}
