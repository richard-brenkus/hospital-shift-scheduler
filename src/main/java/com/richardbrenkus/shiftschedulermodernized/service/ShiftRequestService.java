package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftPreferenceForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ShiftRequestValidationResult;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ShiftRequestMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.ShiftRequestRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class ShiftRequestService {

    UserRepository userRepository;
    ShiftRequestRepository shiftRequestRepository;
    UserService userService;
    UserIndexPageService userIndexPageService;
    ShiftRequestMapper shiftRequestMapper;

    public ShiftRequestService(UserRepository userRepository, ShiftRequestRepository shiftRequestRepository, UserService userService, UserIndexPageService userIndexPageService, ShiftRequestMapper shiftRequestMapper) {
        this.userRepository = userRepository;
        this.shiftRequestRepository = shiftRequestRepository;
        this.userService = userService;
        this.userIndexPageService = userIndexPageService;
        this.shiftRequestMapper = shiftRequestMapper;
    }

    public ShiftRequestValidationResult validateShiftRequest(ShiftRequestForm form) {

        ShiftRequestValidationResult noShiftsOnly =
                validateNoShiftsOnly(form);

        if (!noShiftsOnly.isValid()) {
            return noShiftsOnly;
        }

        ShiftRequestValidationResult conflictingDates =
                validateConflictingDates(form);

        if (!conflictingDates.isValid()) {
            return conflictingDates;
        }

        return validateAllPreferences(form);
    }

    public void applyDefaultUserPriorities(ShiftRequestForm form) {
        form.getPreferences().forEach(preference -> preference.setPriority(5));
    }

    @Transactional
    public ShiftRequest submitShiftRequest(User user, ShiftRequestForm form) {
        ShiftRequest existingRequest = user.getShiftRequest();

        ShiftRequest shiftRequest;

        if (existingRequest == null) {
            shiftRequest = shiftRequestMapper.formToEntity(form);
            //shiftRequest.setUser(user);
            user.setShiftRequest(shiftRequest);
            user.setShiftRequestActive(true);
        } else {
            shiftRequest = this.updateEntity(existingRequest, form);
        }

        user.setShiftRequestActive(true);

        return shiftRequestRepository.save(shiftRequest);
    }

    private ShiftRequestValidationResult validateNoShiftsOnly(ShiftRequestForm form) {

        List<String> rejectedFields = new ArrayList<>();

        for (int i = 0; i < form.getPreferences().size(); i++) {
            ShiftPreferenceForm preference = form.getPreferences().get(i);

            if (preference.isNoShiftRequested()) {
                rejectedFields.add("preferences[" + i + "].noShiftRequested");
            }
        }

        boolean allSubmittedPreferencesAreNoShift =
                !form.getPreferences().isEmpty()
                        && rejectedFields.size() == form.getPreferences().size();

        if (allSubmittedPreferencesAreNoShift) {
            return ShiftRequestValidationResult.invalid(
                    "noShiftsOnlySelected",
                    rejectedFields.toArray(String[]::new)
            );
        }

        return ShiftRequestValidationResult.valid();
    }

    private ShiftRequestValidationResult validateConflictingDates(ShiftRequestForm form) {

        Map<LocalDate, List<String>> dateToFields = new HashMap<>();

        for (LocalDate date : emptyIfNull(form.getDatesNo())) {
            dateToFields
                    .computeIfAbsent(date, key -> new ArrayList<>())
                    .add("datesNo");
        }

        for (int i = 0; i < form.getPreferences().size(); i++) {
            ShiftPreferenceForm preference = form.getPreferences().get(i);

            for (LocalDate date : emptyIfNull(preference.getDatesYes())) {
                dateToFields
                        .computeIfAbsent(date, key -> new ArrayList<>())
                        .add("preferences[" + i + "].datesYes");
            }
        }

        List<String> rejectedFields = dateToFields.values().stream()
                .filter(fields -> fields.size() > 1)
                .flatMap(List::stream)
                .distinct()
                .toList();

        if (!rejectedFields.isEmpty()) {
            return ShiftRequestValidationResult.invalid(
                    "conflictingDates",
                    rejectedFields.toArray(String[]::new)
            );
        }

        return ShiftRequestValidationResult.valid();
    }

    private ShiftRequestValidationResult validateAllPreferences(ShiftRequestForm form) {

        for (int i = 0; i < form.getPreferences().size(); i++) {
            ShiftPreferenceForm preference = form.getPreferences().get(i);

            ShiftRequestValidationResult result =
                    validateSingleShiftPreference(form, preference, i);

            if (!result.isValid()) {
                return result;
            }
        }

        return ShiftRequestValidationResult.valid();
    }

    private ShiftRequestValidationResult validateSingleShiftPreference(
            ShiftRequestForm form,
            ShiftPreferenceForm preference,
            int index) {

        boolean noShift = preference.isNoShiftRequested();
        boolean anyDate = preference.isAnyDateSelected();

        List<LocalDate> datesYes = emptyIfNull(preference.getDatesYes());
        List<LocalDate> datesNo = emptyIfNull(form.getDatesNo());

        int weekdayCount = preference.getWeekdayCount();
        int weekendCount = preference.getWeekendCount();

        String prefix = "preferences[" + index + "]";

        if (noShift && (anyDate || !datesYes.isEmpty())) {
            return ShiftRequestValidationResult.invalid(
                    "invalidInputCondition1",
                    prefix + ".noShiftRequested",
                    prefix + ".anyDateSelected",
                    prefix + ".datesYes"
            );
        }

        if (!noShift && datesYes.isEmpty() && datesNo.isEmpty() && !anyDate) {
            return ShiftRequestValidationResult.invalid(
                    "invalidInputCondition2",
                    prefix + ".datesYes",
                    prefix + ".anyDateSelected"
            );
        }

        if (!noShift && weekdayCount == 0 && weekendCount == 0) {
            return ShiftRequestValidationResult.invalid(
                    "shiftAndWeekendCount",
                    prefix + ".weekdayCount",
                    prefix + ".weekendCount"
            );
        }

        if (!noShift && !datesYes.isEmpty() && anyDate) {
            return ShiftRequestValidationResult.invalid(
                    "yesDatesAnyDate",
                    prefix + ".datesYes",
                    prefix + ".anyDateSelected"
            );
        }

        if (!noShift && !datesNo.isEmpty() && datesYes.isEmpty() && !anyDate) {
            return ShiftRequestValidationResult.invalid(
                    "noDatesOnly",
                    "datesNo",
                    prefix + ".datesYes",
                    prefix + ".anyDateSelected"
            );
        }

        return ShiftRequestValidationResult.valid();
    }

    private List<LocalDate> emptyIfNull(List<LocalDate> dates) {
        return dates == null ? Collections.emptyList() : dates;
    }

    public ShiftRequest updateEntity(ShiftRequest existingRequest, @NotNull ShiftRequestForm form) {

        if (form.getDatesNo() != null)
            existingRequest.setDatesNo(new ArrayList<>(emptyIfNull(form.getDatesNo())));

        for (ShiftPreferenceForm preferenceForm : form.getPreferences()) {
            ShiftPreference existingPreference = findPreferenceByShiftType(existingRequest, preferenceForm.getShiftType());

            if (existingPreference == null) {
                ShiftPreference newPreference = shiftRequestMapper.preferenceFormToEntity(preferenceForm);
                newPreference.setShiftRequest(existingRequest);
                existingRequest.getPreferences().add(newPreference);
                continue;
            }

            updatePreferenceIfChanged(existingPreference, preferenceForm);
        }

        return existingRequest;
    }

    private void updatePreference(ShiftPreference existingPreference,
                                  ShiftPreferenceForm form) {

        existingPreference.setPriority(form.getPriority());

        existingPreference.setDatesYes(new ArrayList<>(emptyIfNull(form.getDatesYes())));
        existingPreference.setNoShiftRequested(form.isNoShiftRequested());
        existingPreference.setAnyDateSelected(form.isAnyDateSelected());
        existingPreference.setWeekdayCount(form.getWeekdayCount());
        existingPreference.setWeekendCount(form.getWeekendCount());

    }

    private ShiftPreference findPreferenceByShiftType(ShiftRequest request, int shiftType) {
        return request.getPreferences()
                .stream()
                .filter(preference -> preference.getShiftType() == shiftType)
                .findFirst()
                .orElse(null);
    }

    private void updatePreferenceIfChanged(ShiftPreference existingPreference,
                                           ShiftPreferenceForm preferenceForm) {

        if (existingPreference.getShiftType() != preferenceForm.getShiftType()) {
            existingPreference.setShiftType(preferenceForm.getShiftType());
        }

        if (existingPreference.isNoShiftRequested() != preferenceForm.isNoShiftRequested()) {
            existingPreference.setNoShiftRequested(preferenceForm.isNoShiftRequested());
        }

        if (existingPreference.isAnyDateSelected() != preferenceForm.isAnyDateSelected()) {
            existingPreference.setAnyDateSelected(preferenceForm.isAnyDateSelected());
        }

        if (existingPreference.getWeekdayCount() != preferenceForm.getWeekdayCount()) {
            existingPreference.setWeekdayCount(preferenceForm.getWeekdayCount());
        }

        if (existingPreference.getWeekendCount() != preferenceForm.getWeekendCount()) {
            existingPreference.setWeekendCount(preferenceForm.getWeekendCount());
        }

        if (existingPreference.getPriority() != preferenceForm.getPriority()) {
            existingPreference.setPriority(preferenceForm.getPriority());
        }

        if (!Objects.equals(existingPreference.getDatesYes(), preferenceForm.getDatesYes())) {
            existingPreference.setDatesYes(new ArrayList<>(preferenceForm.getDatesYes()));
        }
    }


}
