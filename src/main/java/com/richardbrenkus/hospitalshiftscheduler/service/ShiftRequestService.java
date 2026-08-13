package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.activity.ActivityPublisher;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ActivityType;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.ShiftPreferenceForm;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.ShiftRequestForm;
import com.richardbrenkus.hospitalshiftscheduler.dto.view.ShiftRequestValidationResult;
import com.richardbrenkus.hospitalshiftscheduler.dto.view.ShiftRequestViewRecord;
import com.richardbrenkus.hospitalshiftscheduler.entity.ShiftPreference;
import com.richardbrenkus.hospitalshiftscheduler.entity.ShiftRequest;
import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import com.richardbrenkus.hospitalshiftscheduler.mapper.ShiftRequestMapper;
import com.richardbrenkus.hospitalshiftscheduler.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShiftRequestService {

    private final UserRepository userRepository;
    private final ShiftRequestMapper shiftRequestMapper;
    private final ActivityPublisher activityPublisher;

    public ShiftRequestValidationResult validateShiftRequest(ShiftRequestForm form) {

        ShiftRequestValidationResult noShiftsOnly = validateNoShiftsOnly(form);

        if (!noShiftsOnly.isValid()) {
            return noShiftsOnly;
        }

        ShiftRequestValidationResult conflictingDates = validateConflictingDates(form);

        if (!conflictingDates.isValid()) {
            return conflictingDates;
        }

        return validateAllPreferences(form);
    }

    public void applyDefaultUserPriorities(ShiftRequestForm form) {
        form.getPreferences().forEach(preference -> preference.setPriority(5));
    }

    @Transactional
    public ShiftRequest submitShiftRequest(String username, ShiftRequestForm form) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("Invalid username: " + username);
        }

        User user = userOptional.get();

        ShiftRequest existingRequest = user.getShiftRequest();
        boolean requestCreated = existingRequest == null;

        ShiftRequest shiftRequest;

        if (requestCreated) {
            shiftRequest = shiftRequestMapper.formToEntity(form);
        } else {
            shiftRequest = updateEntity(existingRequest, form);
        }

        user.setShiftRequest(shiftRequest);

        /*
         * Flush before publishing so that persistence and constraint failures occur
         * before the success event is registered for AFTER_COMMIT processing.
         */
        User savedUser = userRepository.saveAndFlush(user);
        ShiftRequest savedRequest = savedUser.getShiftRequest();

        ActivityType activityType = requestCreated ? ActivityType.SHIFT_REQUEST_CREATED : ActivityType.SHIFT_REQUEST_UPDATED;

        activityPublisher.publishSuccess(activityType, "ShiftRequest", savedRequest != null && savedRequest.getShiftRequestId() != null ? savedRequest.getShiftRequestId().toString() : username, requestCreated ? "Shift request created" : "Shift request updated");

        return savedRequest;
    }

    @Transactional
    public void deleteShiftRequest(long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Invalid user Id: " + userId));

        this.deleteShiftRequest(user);
    }

    @Transactional
    public void deleteShiftRequest(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("Invalid username: " + username);
        }

        User user = userOptional.get();

        this.deleteShiftRequest(user);
    }

    @Transactional
    public void deleteAllShiftRequests() {
        List<User> users = userRepository.findByShiftRequestIsNotNullOrderByNameAsc();

        long deletedRequestCount = users.size();

        if (deletedRequestCount == 0) {
            return;
        }

        users.forEach(user -> user.setShiftRequest(null));
        userRepository.saveAllAndFlush(users);

        activityPublisher.publishSuccess(ActivityType.ALL_SHIFT_REQUESTS_DELETED, "ShiftRequestBatch", "ALL", "All shift requests deleted; affected requests: " + deletedRequestCount);
    }

    private ShiftRequestValidationResult validateNoShiftsOnly(ShiftRequestForm form) {

        List<String> rejectedFields = new ArrayList<>();

        for (int i = 0; i < form.getPreferences().size(); i++) {
            ShiftPreferenceForm preference = form.getPreferences().get(i);

            if (preference.isNoShiftRequested()) {
                rejectedFields.add("preferences[" + i + "].noShiftRequested");
            }
        }

        boolean allSubmittedPreferencesAreNoShift = !form.getPreferences().isEmpty() && rejectedFields.size() == form.getPreferences().size();

        if (allSubmittedPreferencesAreNoShift) {
            return ShiftRequestValidationResult.invalid("noShiftsOnlySelected", rejectedFields.toArray(String[]::new));
        }

        return ShiftRequestValidationResult.valid();
    }

    private ShiftRequestValidationResult validateConflictingDates(ShiftRequestForm form) {

        Map<LocalDate, List<String>> dateToFields = new HashMap<>();

        for (LocalDate date : emptyIfNull(form.getDatesNo())) {
            dateToFields.computeIfAbsent(date, key -> new ArrayList<>()).add("datesNo");
        }

        for (int i = 0; i < form.getPreferences().size(); i++) {
            ShiftPreferenceForm preference = form.getPreferences().get(i);

            for (LocalDate date : emptyIfNull(preference.getDatesYes())) {
                dateToFields.computeIfAbsent(date, key -> new ArrayList<>()).add("preferences[" + i + "].datesYes");
            }
        }

        List<String> rejectedFields = dateToFields.values().stream().filter(fields -> fields.size() > 1).flatMap(List::stream).distinct().toList();

        if (!rejectedFields.isEmpty()) {
            return ShiftRequestValidationResult.invalid("conflictingDates", rejectedFields.toArray(String[]::new));
        }

        return ShiftRequestValidationResult.valid();
    }

    private ShiftRequestValidationResult validateAllPreferences(ShiftRequestForm form) {

        for (int i = 0; i < form.getPreferences().size(); i++) {
            ShiftPreferenceForm preference = form.getPreferences().get(i);

            ShiftRequestValidationResult result = validateSingleShiftPreference(form, preference, i);

            if (!result.isValid()) {
                return result;
            }
        }

        return ShiftRequestValidationResult.valid();
    }

    private ShiftRequestValidationResult validateSingleShiftPreference(ShiftRequestForm form, ShiftPreferenceForm preference, int index) {

        boolean noShift = preference.isNoShiftRequested();
        boolean anyDate = preference.isAnyDateSelected();

        List<LocalDate> datesYes = emptyIfNull(preference.getDatesYes());
        List<LocalDate> datesNo = emptyIfNull(form.getDatesNo());

        int weekdayCount = preference.getWeekdayCount();
        int weekendCount = preference.getWeekendCount();

        String prefix = "preferences[" + index + "]";

        if (noShift && (anyDate || !datesYes.isEmpty())) {
            return ShiftRequestValidationResult.invalid("invalidInputCondition1", prefix + ".noShiftRequested", prefix + ".anyDateSelected", prefix + ".datesYes");
        }

        if (!noShift && datesYes.isEmpty() && datesNo.isEmpty() && !anyDate) {
            return ShiftRequestValidationResult.invalid("invalidInputCondition2", prefix + ".datesYes", prefix + ".anyDateSelected");
        }

        if (!noShift && weekdayCount == 0 && weekendCount == 0) {
            return ShiftRequestValidationResult.invalid("shiftAndWeekendCount", prefix + ".weekdayCount", prefix + ".weekendCount");
        }

        if (!noShift && !datesYes.isEmpty() && anyDate) {
            return ShiftRequestValidationResult.invalid("yesDatesAnyDate", prefix + ".datesYes", prefix + ".anyDateSelected");
        }

        if (!noShift && !datesNo.isEmpty() && datesYes.isEmpty() && !anyDate) {
            return ShiftRequestValidationResult.invalid("noDatesOnly", "datesNo", prefix + ".datesYes", prefix + ".anyDateSelected");
        }

        return ShiftRequestValidationResult.valid();
    }

    private List<LocalDate> emptyIfNull(List<LocalDate> dates) {
        return dates == null ? Collections.emptyList() : dates;
    }

    public ShiftRequest updateEntity(ShiftRequest existingRequest, @NotNull ShiftRequestForm form) {

        if (form.getDatesNo() != null) {
            existingRequest.setDatesNo(new ArrayList<>(emptyIfNull(form.getDatesNo())));
        }

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

    @Transactional(readOnly = true)
    public ShiftRequestForm getShiftRequestFormByUserId(long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid user Id: " + id));

        return createShiftRequestForm(user);
    }

    @Transactional(readOnly = true)
    public ShiftRequestForm getShiftRequestForm(String username) {

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("Invalid username: " + username);
        }

        User user = userOptional.get();

        return createShiftRequestForm(user);
    }

    private ShiftRequestForm createShiftRequestForm(User user) {
        ShiftRequestForm form = new ShiftRequestForm();

        if (user.getShiftRequest() != null) {
            return shiftRequestMapper.entityToForm(user.getShiftRequest());
        }

        fillAllowedShiftTypes(user, form);
        return form;
    }

    @Transactional(readOnly = true)
    public Optional<ShiftRequestViewRecord> getShiftRequestViewRecord(String username) {

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("Invalid username: " + username);
        }

        User user = userOptional.get();

        if (!user.hasShiftRequest()) {
            return Optional.empty();
        }

        return Optional.of(shiftRequestMapper.entityToViewRecord(user, user.getShiftRequest()));
    }

    private ShiftPreference findPreferenceByShiftType(ShiftRequest request, int shiftType) {
        return request.getPreferences().stream().filter(preference -> preference.getShiftType() == shiftType).findFirst().orElse(null);
    }

    private void updatePreferenceIfChanged(ShiftPreference existingPreference, ShiftPreferenceForm preferenceForm) {

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
            existingPreference.setDatesYes(new ArrayList<>(emptyIfNull(preferenceForm.getDatesYes())));
        }
    }

    private void fillAllowedShiftTypes(User currentUser, ShiftRequestForm shiftRequestForm) {
        List<Integer> allowedShiftTypes = new ArrayList<>(currentUser.getAllowedShiftTypes());

        for (Integer shiftType : allowedShiftTypes) {
            ShiftPreferenceForm shiftPreferenceForm = new ShiftPreferenceForm();
            shiftPreferenceForm.setShiftType(shiftType);
            shiftRequestForm.getPreferences().add(shiftPreferenceForm);
        }
    }

    private void deleteShiftRequest(User user) {
        ShiftRequest existingRequest = user.getShiftRequest();

        if (existingRequest == null) {
            return;
        }

        String shiftRequestId = existingRequest.getShiftRequestId() != null ? existingRequest.getShiftRequestId().toString() : "user:" + user.getId();

        user.setShiftRequest(null);
        userRepository.saveAndFlush(user);

        activityPublisher.publishSuccess(ActivityType.SHIFT_REQUEST_DELETED, "ShiftRequest", shiftRequestId, "Shift request deleted");
    }
}