package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.activity.ActivityPublisher;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.ShiftPreferenceForm;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.ShiftRequestForm;
import com.richardbrenkus.hospitalshiftscheduler.dto.view.ShiftRequestValidationResult;
import com.richardbrenkus.hospitalshiftscheduler.dto.view.ShiftRequestViewRecord;
import com.richardbrenkus.hospitalshiftscheduler.entity.ShiftPreference;
import com.richardbrenkus.hospitalshiftscheduler.entity.ShiftRequest;
import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import com.richardbrenkus.hospitalshiftscheduler.mapper.ShiftRequestMapper;
import com.richardbrenkus.hospitalshiftscheduler.repository.UserRepository;
import com.richardbrenkus.hospitalshiftscheduler.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftRequestServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private ShiftRequestMapper shiftRequestMapper;

    @Mock
    private ActivityPublisher activityPublisher;

    @InjectMocks
    private ShiftRequestService service;

    @Test
    void shouldReturnValid_whenAtLeastOnePreferenceIsNonNoShiftAndOtherwiseValid() {
        ShiftRequestForm form = new ShiftRequestForm();
        form.getPreferences().add(preference(1, false, false, 1, 0, List.of(LocalDate.of(2026, 8, 3))));

        ShiftRequestValidationResult result = service.validateShiftRequest(form);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldReturnNoShiftsOnly_whenAllPreferencesAreNoShiftRequested() {
        ShiftRequestForm form = new ShiftRequestForm();
        form.getPreferences().add(noShiftPreference(1));
        form.getPreferences().add(noShiftPreference(2));

        ShiftRequestValidationResult result = service.validateShiftRequest(form);

        assertThat(result.isValid()).isFalse();
        assertThat(result.modelFlag()).isEqualTo("noShiftsOnlySelected");
    }

    @Test
    void shouldReturnValid_whenPreferencesAreEmpty() {
        ShiftRequestForm form = new ShiftRequestForm();

        ShiftRequestValidationResult result = service.validateShiftRequest(form);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldReturnConflictingDates_whenSameDateAppearsInDatesNoAndDatesYes() {
        LocalDate conflict = LocalDate.of(2026, 8, 5);
        ShiftRequestForm form = new ShiftRequestForm();
        form.setDatesNo(new ArrayList<>(List.of(conflict)));
        form.getPreferences().add(preference(1, false, false, 1, 0, List.of(conflict)));

        ShiftRequestValidationResult result = service.validateShiftRequest(form);

        assertThat(result.isValid()).isFalse();
        assertThat(result.modelFlag()).isEqualTo("conflictingDates");
        assertThat(result.rejectedFields()).contains("datesNo", "preferences[0].datesYes");
    }

    @Test
    void shouldReturnInvalidInputCondition1_whenNoShiftAndAnyDateSelected() {
        ShiftRequestForm form = new ShiftRequestForm();
        form.getPreferences().add(preference(1, true, true, 0, 0, List.of()));
        form.getPreferences().add(preference(2, false, true, 1, 0, List.of()));

        ShiftRequestValidationResult result = service.validateShiftRequest(form);

        assertThat(result.isValid()).isFalse();
        assertThat(result.modelFlag()).isEqualTo("invalidInputCondition1");
    }

    @Test
    void shouldReturnInvalidInputCondition2_whenNoDatesAndNoShiftFlagsAndNoAnyDate() {
        ShiftRequestForm form = new ShiftRequestForm();
        form.getPreferences().add(preference(1, false, false, 1, 0, List.of()));

        ShiftRequestValidationResult result = service.validateShiftRequest(form);

        assertThat(result.isValid()).isFalse();
        assertThat(result.modelFlag()).isEqualTo("invalidInputCondition2");
    }

    @Test
    void shouldReturnShiftAndWeekendCountError_whenBothCountsAreZeroAndDatesPresent() {
        ShiftRequestForm form = new ShiftRequestForm();
        form.getPreferences().add(preference(1, false, false, 0, 0, List.of(LocalDate.of(2026, 8, 3))));

        ShiftRequestValidationResult result = service.validateShiftRequest(form);

        assertThat(result.isValid()).isFalse();
        assertThat(result.modelFlag()).isEqualTo("shiftAndWeekendCount");
    }

    @Test
    void shouldReturnYesDatesAnyDateError_whenBothYesDatesAndAnyDateAreSet() {
        ShiftRequestForm form = new ShiftRequestForm();
        form.getPreferences().add(preference(1, false, true, 1, 0, List.of(LocalDate.of(2026, 8, 3))));

        ShiftRequestValidationResult result = service.validateShiftRequest(form);

        assertThat(result.isValid()).isFalse();
        assertThat(result.modelFlag()).isEqualTo("yesDatesAnyDate");
    }

    @Test
    void shouldReturnNoDatesOnlyError_whenOnlyDatesNoProvidedAndNoYesDates() {
        ShiftRequestForm form = new ShiftRequestForm();
        form.setDatesNo(new ArrayList<>(List.of(LocalDate.of(2026, 8, 5))));
        form.getPreferences().add(preference(1, false, false, 1, 0, List.of()));

        ShiftRequestValidationResult result = service.validateShiftRequest(form);

        assertThat(result.isValid()).isFalse();
        assertThat(result.modelFlag()).isEqualTo("noDatesOnly");
    }

    @Test
    void shouldSetAllPreferencesPriorityToFive_whenApplyingDefaults() {
        ShiftRequestForm form = new ShiftRequestForm();
        form.getPreferences().add(preference(1, false, false, 1, 0, List.of()));
        form.getPreferences().add(preference(2, false, false, 2, 0, List.of()));

        service.applyDefaultUserPriorities(form);

        assertThat(form.getPreferences()).allMatch(preference -> preference.getPriority() == 5);
    }

    @Test
    void shouldThrowUsernameNotFound_whenGettingViewRecordForNonExistentUser() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getShiftRequestViewRecord("ghost")).isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Test
    void shouldReturnEmptyOptional_whenUserHasNoShiftRequest() {
        when(userRepository.findByUsername("mick")).thenReturn(Optional.of(TestFixtures.user(1L, "mick")));

        Optional<ShiftRequestViewRecord> result = service.getShiftRequestViewRecord("mick");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnMappedViewRecord_whenUserHasShiftRequest() {
        User user = TestFixtures.user(1L, "mick");
        ShiftRequest request = new ShiftRequest();
        user.setShiftRequest(request);
        ShiftRequestViewRecord mapped = ShiftRequestViewRecord.builder().stringDatesNo("").build();
        when(userRepository.findByUsername("mick")).thenReturn(Optional.of(user));
        when(shiftRequestMapper.entityToViewRecord(user, request)).thenReturn(mapped);

        assertThat(service.getShiftRequestViewRecord("mick")).contains(mapped);
    }

    @Test
    void shouldReturnFormFromMapper_whenUserHasShiftRequest() {
        User user = TestFixtures.user(1L, "freddie");
        ShiftRequest request = new ShiftRequest();
        user.setShiftRequest(request);
        ShiftRequestForm mappedForm = new ShiftRequestForm();
        when(userRepository.findByUsername("freddie")).thenReturn(Optional.of(user));
        when(shiftRequestMapper.entityToForm(request)).thenReturn(mappedForm);

        assertThat(service.getShiftRequestForm("freddie")).isSameAs(mappedForm);
    }

    @Test
    void shouldPreloadAllowedShiftTypesAsEmptyPreferences_whenUserHasNoShiftRequest() {
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 1, 3);
        when(userRepository.findByUsername("freddie")).thenReturn(Optional.of(user));

        ShiftRequestForm form = service.getShiftRequestForm("freddie");

        assertThat(form.getPreferences()).extracting(ShiftPreferenceForm::getShiftType).containsExactlyInAnyOrder(1, 3);
    }

    @Test
    void shouldReturnFormForUserId_whenUserExists() {
        User user = TestFixtures.user(1L, "freddie");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(service.getShiftRequestFormByUserId(1L)).isNotNull();
    }

    @Test
    void shouldSetShiftRequestToNull_whenDeletingByUsername() {
        User user = TestFixtures.user(1L, "freddie");
        user.setShiftRequest(new ShiftRequest());
        when(userRepository.findByUsername("freddie")).thenReturn(Optional.of(user));

        service.deleteShiftRequest("freddie");

        assertThat(user.getShiftRequest()).isNull();
    }

    @Test
    void shouldReplaceDatesNoOnExistingRequest_whenUpdatingEntity() {
        ShiftRequest existing = new ShiftRequest();
        existing.setDatesNo(new ArrayList<>(List.of(LocalDate.of(2026, 8, 1))));
        ShiftPreference existingPref = ShiftPreference.builder().shiftType(1).priority(3).weekdayCount(2).weekendCount(0).datesYes(new ArrayList<>()).build();
        existingPref.setShiftRequest(existing);
        existing.getPreferences().add(existingPref);

        ShiftRequestForm form = new ShiftRequestForm();
        form.setDatesNo(new ArrayList<>(List.of(LocalDate.of(2026, 8, 5))));
        ShiftPreferenceForm updatedPref = preference(1, false, false, 5, 1, List.of(LocalDate.of(2026, 8, 10)));
        updatedPref.setPriority(2);
        form.getPreferences().add(updatedPref);

        ShiftRequest updated = service.updateEntity(existing, form);

        assertThat(updated.getDatesNo()).containsExactly(LocalDate.of(2026, 8, 5));
        assertThat(existingPref.getWeekdayCount()).isEqualTo(5);
        assertThat(existingPref.getWeekendCount()).isEqualTo(1);
        assertThat(existingPref.getPriority()).isEqualTo(2);
        assertThat(existingPref.getDatesYes()).containsExactly(LocalDate.of(2026, 8, 10));
    }

    @Test
    void shouldCreateNewPreferenceOnExistingRequest_whenUpdatingWithUnknownShiftType() {
        ShiftRequest existing = new ShiftRequest();
        ShiftPreferenceForm newPref = preference(5, false, true, 1, 0, List.of());
        ShiftRequestForm form = new ShiftRequestForm();
        form.getPreferences().add(newPref);
        ShiftPreference mappedEntity = ShiftPreference.builder().shiftType(5).priority(1).weekdayCount(1).weekendCount(0).anyDateSelected(true).datesYes(new ArrayList<>()).build();
        when(shiftRequestMapper.preferenceFormToEntity(newPref)).thenReturn(mappedEntity);

        ShiftRequest updated = service.updateEntity(existing, form);

        assertThat(updated.getPreferences()).containsExactly(mappedEntity);
        assertThat(mappedEntity.getShiftRequest()).isSameAs(existing);
    }

    private static ShiftPreferenceForm preference(int shiftType, boolean noShift, boolean anyDate, int weekdayCount, int weekendCount, List<LocalDate> datesYes) {
        ShiftPreferenceForm form = new ShiftPreferenceForm();
        form.setShiftType(shiftType);
        form.setNoShiftRequested(noShift);
        form.setAnyDateSelected(anyDate);
        form.setWeekdayCount(weekdayCount);
        form.setWeekendCount(weekendCount);
        form.setPriority(1);
        form.setDatesYes(new ArrayList<>(datesYes));
        return form;
    }

    private static ShiftPreferenceForm noShiftPreference(int shiftType) {
        ShiftPreferenceForm form = new ShiftPreferenceForm();
        form.setShiftType(shiftType);
        form.setNoShiftRequested(true);
        form.setDatesYes(new ArrayList<>());
        return form;
    }
}
