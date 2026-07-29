package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.algorithm.record.*;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.UserCalculationDataMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculatedScheduleConverterTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCalculationDataMapper userCalculationDataMapper;

    @InjectMocks
    private CalculatedScheduleConverter converter;

    @Test
    void shouldConvertCalculatedCandidateToLegacyScheduleMonth() {
        User userOne = user(1L, "Freddie Mercury");
        User userTwo = user(2L, "David Bowie");

        CalculatedScheduleMonth calculated = CalculatedScheduleMonth.builder().month(AUGUST_2026).hitCounter(2).days(new ArrayList<>(List.of(day(LocalDate.of(2026, 8, 1), true, new CalculatedShiftAssignment(1, 1L)), day(LocalDate.of(2026, 8, 2), true, new CalculatedShiftAssignment(2, 2L))))).build();

        CalculationProfileForm form = CalculationProfileForm.builder().calculationMonth(AUGUST_2026).shiftCountCap(10).gapBetweenShifts(5).forceFillShiftTypes(List.of(1)).build();

        UserCalculationData ucdOne = calculationData(1L);
        UserCalculationData ucdTwo = calculationData(2L);

        when(userRepository.findAllById(any())).thenReturn(List.of(userOne, userTwo));
        when(userCalculationDataMapper.toCalculationData(eq(userOne), any())).thenReturn(ucdOne);
        when(userCalculationDataMapper.toCalculationData(eq(userTwo), any())).thenReturn(ucdTwo);

        ScheduleMonth result = converter.toLegacyScheduleMonth(ScheduleCandidate.from(calculated, 1, 7, 123L), form);

        assertThat(result.getMonth()).isEqualTo(AUGUST_2026);
        assertThat(result.getHitCounter()).isEqualTo(2);
        assertThat(result.getCalculationProfile()).isSameAs(form);
        assertThat(result.getDays()).hasSize(2);

        assertThat(result.getDays().get(0).getAssignments().getFirst().getShiftType()).isEqualTo(1);
        assertThat(result.getDays().get(0).getAssignments().getFirst().getUserCalculationData()).isSameAs(ucdOne);
        assertThat(result.getDays().get(1).getAssignments().getFirst().getUserCalculationData()).isSameAs(ucdTwo);

        assertThat(result.isOverrideUserShiftRequestExceptNoDates()).isFalse();
        assertThat(result.isOverrideUserShiftRequestAll()).isFalse();
        assertThat(result.isOverrideShiftCountCap()).isFalse();
        assertThat(result.isOverrideConflictingDates()).isFalse();
        assertThat(result.isOverrideHasShiftRequest()).isFalse();
        assertThat(result.isOverridePreviousMonthValid()).isFalse();

        verify(userRepository).findAllById(argThat(ids -> {
            java.util.Set<Long> set = new java.util.HashSet<>();
            ids.forEach(set::add);
            return set.equals(Set.of(1L, 2L));
        }));
    }

    @Test
    void shouldThrowWhenCalculatedAssignmentReferencesMissingUser() {
        CalculatedScheduleMonth calculated = CalculatedScheduleMonth.builder().month(AUGUST_2026).hitCounter(1).days(new ArrayList<>(List.of(day(LocalDate.of(2026, 8, 1), true, new CalculatedShiftAssignment(1, 999L))))).build();

        when(userRepository.findAllById(any())).thenReturn(List.of());
        // The converter passes usersById.get(999L) → null → mapper returns null →
        // converter throws IllegalStateException.
        when(userCalculationDataMapper.toCalculationData(eq(null), any())).thenReturn(null);

        assertThatThrownBy(() -> converter.toLegacyScheduleMonth(ScheduleCandidate.from(calculated, 0, 0, 1L), CalculationProfileForm.builder().calculationMonth(AUGUST_2026).build())).isInstanceOf(IllegalStateException.class).hasMessageContaining("999");
    }

    @Test
    void shouldConvertEmptyCalculatedSchedule() {
        CalculatedScheduleMonth calculated = CalculatedScheduleMonth.builder().month(AUGUST_2026).hitCounter(0).days(new ArrayList<>()).build();

        when(userRepository.findAllById(any())).thenReturn(List.of());

        ScheduleMonth result = converter.toLegacyScheduleMonth(ScheduleCandidate.from(calculated, 0, 0, 1L), CalculationProfileForm.builder().calculationMonth(AUGUST_2026).build());

        assertThat(result.getDays()).isEmpty();
        assertThat(result.getHitCounter()).isZero();
    }

    private CalculatedScheduleDay day(LocalDate date, boolean weekend, CalculatedShiftAssignment assignment) {
        return CalculatedScheduleDay.builder().date(date).weekendOrHoliday(weekend).assignments(new ArrayList<>(List.of(assignment))).build();
    }

    private User user(Long id, String name) {
        return User.builder().id(id).name(name).username(name.toLowerCase().replace(" ", ".")).email(name.toLowerCase().replace(" ", ".") + "@test.local").password("encoded").enabled(true).build();
    }

    private UserCalculationData calculationData(long id) {
        return new UserCalculationData(id, "User " + id, "user-" + id, null, Set.of(1, 2), Set.of(), Map.of(), Set.of(), true);
    }
}
