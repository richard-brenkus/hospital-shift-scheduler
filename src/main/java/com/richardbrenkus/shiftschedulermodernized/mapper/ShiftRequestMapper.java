package com.richardbrenkus.shiftschedulermodernized.mapper;

import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftPreferenceForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;

import java.util.ArrayList;
import java.util.List;

public class ShiftRequestMapper {

    public ShiftRequest map(ShiftRequestForm shiftRequestForm) {
        ShiftRequest shiftRequest = new ShiftRequest();
        shiftRequest.getDatesNo().clear();
        shiftRequest.getDatesNo().addAll(new ArrayList<>(shiftRequestForm.getDatesNo()));
        shiftRequest.getPreferences().clear();
        shiftRequest.getPreferences().addAll(this.transformShiftPreferences(shiftRequestForm.getPreferences())));

        return shiftRequest;
    }

    private ArrayList<ShiftPreference> transformShiftPreferences(List<ShiftPreferenceForm> shiftPreferenceFormList) {
        List<ShiftPreference> shiftPreferences = new ArrayList<>();
        shiftPreferenceFormList.forEach(shiftPreferenceForm ->{
                    ShiftPreference shiftPreference = new ShiftPreference(shiftPreferenceForm);
                    shiftPreference.setShiftUnwanted(shiftPreferenceForm.isShiftUnwanted());
                    shiftPreference.setAnyDateSelectionAllowed(shiftPreferenceForm.isAnyDateSelectionAllowed());
                    shiftPreference.setDatesYes(shiftPreferenceForm.getDatesYes());

                    shiftPreferences.add(shiftPreference);
                }

                shiftPreferences.add(new ShiftPreference(shiftPreferenceForm)));
        return localDates;
    }
}
