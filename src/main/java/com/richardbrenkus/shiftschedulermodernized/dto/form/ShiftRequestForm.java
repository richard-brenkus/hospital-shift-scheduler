package com.richardbrenkus.shiftschedulermodernized.dto.form;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class ShiftRequestForm {
    private List<LocalDate> datesNo = new ArrayList<>();
    private List<ShiftPreferenceForm> preferences = new ArrayList<>();
    private boolean datesNoUpdate;
    private Set<Integer> allowedShiftTypes = new HashSet<>();
}
