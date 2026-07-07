package com.richardbrenkus.shiftschedulermodernized.mapper;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ApplicationConstants;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftPreferenceForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ShiftPreferenceViewRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ShiftRequestViewRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.richardbrenkus.shiftschedulermodernized.config.constants.ApplicationConstants.DATE_FORMATTER;

@Component
public class ShiftRequestMapper {

    public ShiftRequest formToEntity(ShiftRequestForm form) {
        ShiftRequest shiftRequest = new ShiftRequest();

        if (form.getDatesNo() != null)
            shiftRequest.setDatesNo(new ArrayList<>(form.getDatesNo()));
        List<ShiftPreference> shiftPreferences = new ArrayList<>();

        for (ShiftPreferenceForm preferenceForm : form.getPreferences()) {
            ShiftPreference shiftPreference = preferenceFormToEntity(preferenceForm);
            shiftPreference.setShiftRequest(shiftRequest);
            shiftPreferences.add(shiftPreference);
        }

        shiftRequest.setPreferences(shiftPreferences);

        return shiftRequest;
    }


    public ShiftRequestForm entityToForm(ShiftRequest shiftRequest) {
        ShiftRequestForm shiftRequestForm = new ShiftRequestForm();
        if (shiftRequest.getDatesNo() != null)
            shiftRequestForm.setDatesNo(new ArrayList<>(shiftRequest.getDatesNo()));
        List<ShiftPreferenceForm> shiftPreferences = new ArrayList<>();

        for (ShiftPreference shiftPreference : shiftRequest.getPreferences()) {
            shiftPreferences.add(preferenceEntityToForm(shiftPreference));
        }

        shiftRequestForm.setPreferences(shiftPreferences);

        return shiftRequestForm;
    }

    public ShiftRequestViewRecord entityToViewRecord(User user, ShiftRequest shiftRequest) {
        List<ShiftPreferenceViewRecord> shiftPreferenceViewRecords = new ArrayList<>();
        if (shiftRequest != null) {
            for (ShiftPreference shiftPreference : shiftRequest.getPreferences()) {
                ShiftPreferenceViewRecord shiftPreferenceViewRecord = ShiftPreferenceViewRecord.builder()
                        .shiftType(shiftPreference.getShiftType())
                        .noShiftRequested(shiftPreference.isNoShiftRequested())
                        .anyDateSelected(shiftPreference.isAnyDateSelected())
                        .weekdayCount(shiftPreference.getWeekdayCount())
                        .weekendCount(shiftPreference.getWeekendCount())
                        .priority(shiftPreference.getPriority())
                        .stringDatesYes(this.getShiftRequestDatesAsString(shiftPreference.getDatesYes()))
                        .build();

                shiftPreferenceViewRecords.add(shiftPreferenceViewRecord);

            }
        }

        List<LocalDate> datesNo = new ArrayList<>();
        if (shiftRequest != null) {
            datesNo = shiftRequest.getDatesNo();
        }

        return ShiftRequestViewRecord.builder()
                .stringDatesNo(this.getShiftRequestDatesAsString(datesNo))
                .enabledShiftTypes(user.getAllowedShiftTypes())
                .preferences(shiftPreferenceViewRecords)
                .build();

    }

    public ShiftPreference preferenceFormToEntity(ShiftPreferenceForm form) {
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

    public ShiftPreferenceForm preferenceEntityToForm(ShiftPreference entity) {

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

    private String formatDates(List<LocalDate> dates) {

        if (dates == null)
            return "";

        String datesString = dates.stream()
                .map(d -> d.format(DateTimeFormatter.ofPattern(ApplicationConstants.DATE_FORMATTER.toString())))
                .collect(Collectors.joining(", "));

        return datesString.substring(1, datesString.length() - 1);
    }

    private String getShiftRequestDatesAsString(List<LocalDate> localDatesList) {

        List<String> stringDatesList = new ArrayList<>();
        String stringDates = "";
        if (localDatesList != null && !localDatesList.isEmpty()) {
            for (LocalDate d : localDatesList) {
                stringDatesList.add(DATE_FORMATTER.format(d));
            }
            stringDates = stringDatesList.toString();
            stringDates = stringDates.substring(1, stringDates.length() - 1);
        }

        return stringDates;
    }


}
