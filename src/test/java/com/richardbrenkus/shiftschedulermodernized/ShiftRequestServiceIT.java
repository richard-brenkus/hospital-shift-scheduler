package com.richardbrenkus.shiftschedulermodernized;

import com.richardbrenkus.shiftschedulermodernized.container.AbstractMySqlContainerTest;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftPreferenceForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.ShiftRequestRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.service.ShiftRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@Sql(scripts = "/sql/user-with-shift-request.sql", executionPhase = BEFORE_TEST_METHOD)
class ShiftRequestServiceIT extends AbstractMySqlContainerTest {

    @Autowired
    private ShiftRequestService shiftRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShiftRequestRepository shiftRequestRepository;

    @Test
    @Transactional
    void shouldCreateShiftRequestForUserWithoutOne_whenSubmittingNewRequest() {
        ShiftRequestForm form = new ShiftRequestForm();
        form.setDatesNo(new ArrayList<>(List.of(LocalDate.of(2026, 8, 20))));
        ShiftPreferenceForm preference = new ShiftPreferenceForm();
        preference.setShiftType(1);
        preference.setPriority(1);
        preference.setWeekdayCount(2);
        preference.setWeekendCount(0);
        preference.setAnyDateSelected(false);
        preference.setDatesYes(new ArrayList<>(List.of(LocalDate.of(2026, 8, 5))));
        form.getPreferences().add(preference);

        shiftRequestService.submitShiftRequest("mick.jagger", form);

        User reloaded = userRepository.findByUsername("mick.jagger").orElseThrow();
        assertThat(reloaded.getShiftRequest()).isNotNull();
        ShiftRequest saved = reloaded.getShiftRequest();
        assertThat(saved.getDatesNo()).containsExactly(LocalDate.of(2026, 8, 20));
        assertThat(saved.getPreferences()).hasSize(1);
        ShiftPreference persistedPreference = saved.getPreferences().getFirst();
        assertThat(persistedPreference.getShiftType()).isEqualTo(1);
        assertThat(persistedPreference.getDatesYes()).containsExactly(LocalDate.of(2026, 8, 5));
    }

    @Test
    @Transactional
    void shouldMergeIntoExistingRequestPreserveOrphansViaCascade_whenSubmittingUpdate() {
        User user = userRepository.findByUsername("freddie.mercury").orElseThrow();
        Long originalRequestId = user.getShiftRequest().getShiftRequestId();

        ShiftRequestForm form = new ShiftRequestForm();
        form.setDatesNo(new ArrayList<>(List.of(LocalDate.of(2026, 8, 25))));
        ShiftPreferenceForm updatedPref1 = new ShiftPreferenceForm();
        updatedPref1.setShiftType(1);
        updatedPref1.setPriority(2);
        updatedPref1.setWeekdayCount(4);
        updatedPref1.setWeekendCount(2);
        updatedPref1.setAnyDateSelected(false);
        updatedPref1.setDatesYes(new ArrayList<>(List.of(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 8))));
        form.getPreferences().add(updatedPref1);

        shiftRequestService.submitShiftRequest("freddie.mercury", form);

        User reloaded = userRepository.findByUsername("freddie.mercury").orElseThrow();
        ShiftRequest reloadedRequest = reloaded.getShiftRequest();
        assertThat(reloadedRequest.getShiftRequestId()).isEqualTo(originalRequestId);
        assertThat(reloadedRequest.getDatesNo()).containsExactly(LocalDate.of(2026, 8, 25));

        ShiftPreference reloadedPref1 = reloadedRequest.getPreferences()
                .stream()
                .filter(preference -> preference.getShiftType() == 1)
                .findFirst()
                .orElseThrow();
        assertThat(reloadedPref1.getWeekdayCount()).isEqualTo(4);
        assertThat(reloadedPref1.getWeekendCount()).isEqualTo(2);
        assertThat(reloadedPref1.getDatesYes()).containsExactlyInAnyOrder(
                LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 8));
    }

    @Test
    @Transactional
    void shouldDeleteShiftRequestAndOrphanRemovePreferences_whenDeletingByUserId() {
        User user = userRepository.findByUsername("freddie.mercury").orElseThrow();
        Long requestId = user.getShiftRequest().getShiftRequestId();

        shiftRequestService.deleteShiftRequest(user.getId());

        User reloaded = userRepository.findByUsername("freddie.mercury").orElseThrow();
        assertThat(reloaded.getShiftRequest()).isNull();
        assertThat(shiftRequestRepository.getShiftRequestByID(requestId)).isNull();
    }

    @Test
    @Transactional
    void shouldDeleteShiftRequestByUsername() {
        User user = userRepository.findByUsername("freddie.mercury").orElseThrow();
        assertThat(user.getShiftRequest()).isNotNull();

        shiftRequestService.deleteShiftRequest("freddie.mercury");

        User reloaded = userRepository.findByUsername("freddie.mercury").orElseThrow();
        assertThat(reloaded.getShiftRequest()).isNull();
    }

    @Test
    @Transactional
    void shouldRemoveAllShiftRequests_whenDeletingAll() {
        shiftRequestService.deleteAllShiftRequests();

        User reloadedFreddie = userRepository.findByUsername("freddie.mercury").orElseThrow();
        assertThat(reloadedFreddie.getShiftRequest()).isNull();
    }

    @Test
    @Transactional
    void shouldReturnFormPopulatedFromDatabase_whenGettingShiftRequestFormForUserWithRequest() {
        ShiftRequestForm form = shiftRequestService.getShiftRequestForm("freddie.mercury");

        assertThat(form.getDatesNo()).containsExactlyInAnyOrder(
                LocalDate.of(2026, 8, 14),
                LocalDate.of(2026, 8, 19)
        );
        assertThat(form.getPreferences()).hasSize(2);
    }
}
