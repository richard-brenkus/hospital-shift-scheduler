package com.richardbrenkus.shiftschedulermodernized.util;

import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;

import java.util.ArrayList;

public class ShiftRequestFormFactory {

    public static ShiftRequestForm createEmptyShiftRequestForm() {

        ShiftRequestForm shiftRequestForm = new ShiftRequestForm();
        shiftRequestForm.setPreferences(new ArrayList<>());
        shiftRequestForm.setDatesNo(new ArrayList<>());

        return shiftRequestForm;
    }
}
