package com.richardbrenkus.shiftschedulermodernized.mapper;

import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftPreferenceForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ShiftPreferenceViewRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ShiftRequestViewRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ShiftRequestMapper {

    public ShiftRequest formToEntity(ShiftRequestForm form) {
        ShiftRequest shiftRequest = new ShiftRequest();
        shiftRequest.setDatesNo(new ArrayList<>(form.getDatesNo()));
        List<ShiftPreference> shiftPreferences = new ArrayList<>();

        for (ShiftPreferenceForm preferenceForm : form.getPreferences()) {
            ShiftPreference shiftPreference = toEntity(preferenceForm);
            shiftPreferences.add(shiftPreference);
        }

        shiftRequest.setPreferences(shiftPreferences);

        return shiftRequest;
    }


    public ShiftRequestForm entityToForm(ShiftRequest shiftRequest) {
        ShiftRequestForm shiftRequestForm = new ShiftRequestForm();
        shiftRequestForm.setDatesNo(new ArrayList<>(shiftRequest.getDatesNo()));
        List<ShiftPreferenceForm> shiftPreferences = new ArrayList<>();

        for (ShiftPreference shiftPreference : shiftRequest.getPreferences()) {
            shiftPreferences.add(toForm(shiftPreference));
        }

        shiftRequestForm.setPreferences(shiftPreferences);

        return shiftRequestForm;
    }

    public ShiftRequestViewRecord entityToViewRecord(ShiftRequest shiftRequest) {
        List<ShiftPreferenceViewRecord> shiftPreferenceViewRecords = new ArrayList<>();
        for (ShiftPreference shiftPreference : shiftRequest.getPreferences()) {
            ShiftPreferenceViewRecord shiftPreferenceViewRecord = ShiftPreferenceViewRecord.builder()
                    .shiftType(shiftPreference.getShiftType())
                    .noShiftRequested(shiftPreference.isNoShiftRequested())
                    .anyDateSelected(shiftPreference.isAnyDateSelected())
                    .weekdayCount(shiftPreference.getWeekdayCount())
                    .weekendCount(shiftPreference.getWeekendCount())
                    .priority(shiftPreference.getPriority())
                    .datesYes(shiftPreference.getDatesYes())
                    .build();

            shiftPreferenceViewRecords.add(shiftPreferenceViewRecord);

        }

        return ShiftRequestViewRecord.builder()
                .datesNo(shiftRequest.getDatesNo())
                .enabledShiftTypes(shiftRequest.getUser().getAllowedShiftTypes())
                .preferences(shiftPreferenceViewRecords)
                .build();

    }

    private ShiftPreference toEntity(ShiftPreferenceForm form) {
        return ShiftPreference.builder()
                .shiftType(form.getShiftType())
                .noShiftRequested(form.isNoShiftRequested())
                .anyDateSelected(form.isAnyDateSelected())
                .weekdayCount(form.getWeekdayCount())
                .weekendCount(form.getWeekendCount())
                .priority(form.getPriority())
                .datesYes(new ArrayList<>(form.getDatesYes()))
                .build();
    }

    private ShiftPreferenceForm toForm(ShiftPreference entity) {
        return ShiftPreferenceForm.builder()
                .shiftType(entity.getShiftType())
                .noShiftRequested(entity.isNoShiftRequested())
                .anyDateSelected(entity.isAnyDateSelected())
                .weekdayCount(entity.getWeekdayCount())
                .weekendCount(entity.getWeekendCount())
                .priority(entity.getPriority())
                .datesYes(new ArrayList<>(entity.getDatesYes()))
                .build();
    }


}
