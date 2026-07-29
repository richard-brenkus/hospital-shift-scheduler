package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.*;
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
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleGenerationEngineTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private ScheduleRuleService scheduleRuleService;

    @InjectMocks
    private ScheduleGenerationEngine engine;

    @Test
    void shouldAssignUserAndIncrementHitCounter_whenAllRulesPass() {
        UserCalculationData user = userData(Set.of(), anyDatePreference(1));
        CalculationInput input = calculationInput(List.of(user), List.of(1), List.of(1), List.of());
        CalculatedScheduleMonth month = emptyCalculatedMonth(AUGUST_2026);
        allowAllDtoRules();

        int hitCounter = engine.assignRegularShifts(month, List.of(3), input, new CalculationCounters(), new Random(1L));

        assertThat(hitCounter).isEqualTo(1);
        assertThat(hasAssignmentOnDay(month, 3, 1)).isTrue();
    }

    @Test
    void shouldReturnZeroHits_whenMinimalGapCheckFails() {
        UserCalculationData user = userData(Set.of(), anyDatePreference(1));
        CalculationInput input = calculationInput(List.of(user), List.of(1), List.of(1), List.of());
        CalculatedScheduleMonth month = emptyCalculatedMonth(AUGUST_2026);

        when(scheduleRuleService.isWithinTotalShiftLimit(anyInt(), any(UserCalculationData.class), any(CalculationCounters.class))).thenReturn(true);
        when(scheduleRuleService.isWithinRequestedWeekdayLimit(any(UserCalculationData.class), anyInt(), any(CalculationCounters.class))).thenReturn(true);
        when(scheduleRuleService.isWithinRequestedWeekendLimit(any(UserCalculationData.class), anyInt(), any(CalculationCounters.class))).thenReturn(true);
        when(scheduleRuleService.respectsMinimalGap(any(LocalDate.class), anyInt(), any(UserCalculationData.class), any(CalculatedScheduleMonth.class), anyInt())).thenReturn(false);
        when(scheduleRuleService.respectsPreviousMonthGap(anyInt(), any(LocalDate.class), any(UserCalculationData.class), any(YearMonth.class))).thenReturn(true);

        int hitCounter = engine.assignRegularShifts(month, List.of(1, 2, 3), input, new CalculationCounters(), new Random(1L));

        assertThat(hitCounter).isZero();
        assertThat(anyAssignmentInMonth(month)).isFalse();
    }

    @Test
    void shouldSkipUserWithoutMatchingPreferenceInMonth() {
        ShiftPreferenceCalculationData preference = new ShiftPreferenceCalculationData(1, 1, 5, 0, false, false, Set.of(LocalDate.of(2026, 12, 1)));

        UserCalculationData user = userData(Set.of(), preference);

        CalculationInput input = calculationInput(List.of(user), List.of(1), List.of(1), List.of());

        CalculatedScheduleMonth month = emptyCalculatedMonth(AUGUST_2026);

        int hitCounter = engine.assignRegularShifts(month, List.of(1), input, new CalculationCounters(), new Random(1L));

        assertThat(hitCounter).isZero();
        assertThat(anyAssignmentInMonth(month)).isFalse();

        verifyNoInteractions(scheduleRuleService);
    }

    @Test
    void shouldOnlyProcessForceFillShiftTypes_whenCallingForceFillPath() {
        UserCalculationData user = userData(Set.of(), anyDatePreference(2));
        CalculationInput input = calculationInput(List.of(user), List.of(1, 2), List.of(1, 2), List.of(2));
        CalculatedScheduleMonth month = emptyCalculatedMonth(AUGUST_2026);
        allowAllDtoRules();

        int hitCounter = engine.assignForceFillShifts(month, List.of(3), input, new CalculationCounters(), new Random(1L));

        assertThat(hitCounter).isEqualTo(1);
        assertThat(hasAssignmentOnDay(month, 3, 2)).isTrue();
        assertThat(hasAssignmentOnDay(month, 3, 1)).isFalse();
    }

    @Test
    void shouldSkipForceFillShiftTypesInRegularPath() {
        UserCalculationData user = userData(Set.of(), anyDatePreference(2));

        CalculationInput input = calculationInput(List.of(user), List.of(2), List.of(2), List.of(2));

        CalculatedScheduleMonth month = emptyCalculatedMonth(AUGUST_2026);

        int hitCounter = engine.assignRegularShifts(month, List.of(3), input, new CalculationCounters(), new Random(1L));

        assertThat(hitCounter).isZero();
        assertThat(anyAssignmentInMonth(month)).isFalse();

        verifyNoInteractions(scheduleRuleService);
    }

    @Test
    void shouldNotAssignUserRejectingTheDate() {
        LocalDate rejectedDate = AUGUST_2026.atDay(3);

        UserCalculationData user = userData(Set.of(rejectedDate), anyDatePreference(1));

        CalculationInput input = calculationInput(List.of(user), List.of(1), List.of(1), List.of());

        CalculatedScheduleMonth month = emptyCalculatedMonth(AUGUST_2026);

        int hitCounter = engine.assignRegularShifts(month, List.of(3), input, new CalculationCounters(), new Random(1L));

        assertThat(hitCounter).isZero();
        assertThat(anyAssignmentInMonth(month)).isFalse();

        verifyNoInteractions(scheduleRuleService);
    }

    @Test
    void shouldOnlyAssignOnce_whenSameShiftTypeIsVisitedMultipleTimes() {
        UserCalculationData user = userData(Set.of(), anyDatePreference(1));
        CalculationInput input = calculationInput(List.of(user), List.of(1), List.of(1, 1), List.of());
        CalculatedScheduleMonth month = emptyCalculatedMonth(AUGUST_2026);
        allowAllDtoRules();

        int hitCounter = engine.assignRegularShifts(month, List.of(3), input, new CalculationCounters(), new Random(1L));

        long assignmentsOnDay = month.getDays().stream().filter(day -> day.getDate().equals(AUGUST_2026.atDay(3))).flatMap(day -> day.getAssignments().stream()).filter(assignment -> assignment.shiftType() == 1).count();

        assertThat(assignmentsOnDay).isEqualTo(1);
        assertThat(hitCounter).isEqualTo(1);
    }

    @Test
    void shouldStoreUserIdInsteadOfJpaUserInCalculatedAssignment() {
        UserCalculationData user = userData(Set.of(), anyDatePreference(1));
        CalculationInput input = calculationInput(List.of(user), List.of(1), List.of(1), List.of());
        CalculatedScheduleMonth month = emptyCalculatedMonth(AUGUST_2026);
        allowAllDtoRules();

        engine.assignRegularShifts(month, List.of(3), input, new CalculationCounters(), new Random(1L));

        assertThat(month.getDays().stream().flatMap(day -> day.getAssignments().stream()).map(CalculatedShiftAssignment::userId)).containsExactly(1L);
    }

    private void allowAllDtoRules() {
        when(scheduleRuleService.isWithinTotalShiftLimit(anyInt(), any(UserCalculationData.class), any(CalculationCounters.class))).thenReturn(true);
        when(scheduleRuleService.isWithinRequestedWeekdayLimit(any(UserCalculationData.class), anyInt(), any(CalculationCounters.class))).thenReturn(true);
        when(scheduleRuleService.isWithinRequestedWeekendLimit(any(UserCalculationData.class), anyInt(), any(CalculationCounters.class))).thenReturn(true);
        when(scheduleRuleService.respectsMinimalGap(any(LocalDate.class), anyInt(), any(UserCalculationData.class), any(CalculatedScheduleMonth.class), anyInt())).thenReturn(true);
        when(scheduleRuleService.respectsPreviousMonthGap(anyInt(), any(LocalDate.class), any(UserCalculationData.class), any(YearMonth.class))).thenReturn(true);
    }

    private UserCalculationData userData(Set<LocalDate> unavailableDates, ShiftPreferenceCalculationData preference) {
        return new UserCalculationData(1L, "Freddie Mercury", "freddie", "MUDr.", Set.of(preference.shiftType()), unavailableDates, Map.of(preference.shiftType(), preference), Set.of(), true);
    }

    private ShiftPreferenceCalculationData anyDatePreference(int shiftType) {
        return new ShiftPreferenceCalculationData(shiftType, 1, 5, 5, false, true, Set.of());
    }

    private CalculationInput calculationInput(List<UserCalculationData> users, List<Integer> shiftTypes, List<Integer> calculationOrder, List<Integer> forceFillShiftTypes) {
        return new CalculationInput(AUGUST_2026, users, shiftTypes, calculationOrder, List.of(1), List.of(), new CalculationProfile(10, 2, false, forceFillShiftTypes));
    }

    private CalculatedScheduleMonth emptyCalculatedMonth(YearMonth month) {
        List<CalculatedScheduleDay> days = IntStream.rangeClosed(1, month.lengthOfMonth()).mapToObj(month::atDay).map(date -> CalculatedScheduleDay.builder().date(date).weekendOrHoliday(date.getDayOfWeek().getValue() >= 6).assignments(new ArrayList<>()).build()).collect(Collectors.toCollection(ArrayList::new));

        return CalculatedScheduleMonth.builder().month(month).hitCounter(0).days(days).build();
    }

    private boolean hasAssignmentOnDay(CalculatedScheduleMonth month, int dayOfMonth, int shiftType) {
        return month.getDays().stream().filter(day -> day.getDate().equals(AUGUST_2026.atDay(dayOfMonth))).flatMap(day -> day.getAssignments().stream()).anyMatch(assignment -> assignment.shiftType() == shiftType);
    }

    private boolean anyAssignmentInMonth(CalculatedScheduleMonth month) {
        return month.getDays().stream().flatMap(day -> day.getAssignments().stream()).findAny().isPresent();
    }
}
