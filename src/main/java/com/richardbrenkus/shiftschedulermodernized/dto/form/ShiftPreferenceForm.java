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
public class ShiftPreferenceForm {
    private int shiftType;
    private boolean shiftUnwanted;
    private boolean anyDateSelectionAllowed;
    private boolean datesYesUpdate;
    private List<LocalDate> datesYes = new ArrayList<>();
    private int shiftPriority;
}
