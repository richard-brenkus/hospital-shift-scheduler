package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.*;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.UserCalculationData;
import com.richardbrenkus.shiftschedulermodernized.config.ShiftTypeProperties;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ScheduleEditForm;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * NOTE: The original generated tests referenced a removed 2-argument
 * ScheduleMapper.toScheduleMonth signature, User-based ScheduleRuleService
 * overloads that no longer exist, and a legacy ScheduleValidationService
 * constructor. They were structurally obsolete after the refactor to
 * UserCalculationData-based rule evaluation. The tests below exercise the
 * still-current public contract of ScheduleValidationService.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleValidationServiceTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private ScheduleRuleService scheduleRuleService;

    @Mock
    private UserStatisticService userStatisticService;

    @Mock
    private UserService userService;

    private ScheduleValidationService service;

    @BeforeEach
    void setUp() {
        service = new ScheduleValidationService(
                new ShiftTypeProperties(6),
                scheduleRuleService,
                userStatisticService,
                userService
        );
    }

    @Test
    void shouldInitializeValidationAndUserStats_whenGivenConvertedWinningSchedule() {
        stubEmptyStatistics();

        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        month.setCalculationProfile(calculationProfile(10, 0));

        ScheduleValidationResult result = service.initializeValidationAndUserStats(month);

        assertThat(result.getScheduleMonth()).isSameAs(month);
        assertThat(result.getScheduleScore()).isEqualTo("0/186");
        assertThat(result.isErrorsExist()).isFalse();
        assertThat(result.isAllUsersExist()).isTrue();
    }

    @Test
    void shouldReturnEmptyResult_whenValidatingEmptyEditForm() {
        ScheduleEditForm form = ScheduleEditForm.builder().build();

        when(userService.getScheduleMonth(any(ScheduleEditForm.class))).thenReturn(null);

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isErrorsExist()).isFalse();
        assertThat(result.isAllUsersExist()).isTrue();
        assertThat(result.getScheduleMonth()).isNull();
    }

    @Test
    void shouldThrowIllegalArgumentException_whenScheduleHasNoCalculationProfile() {
        ScheduleEditForm form = ScheduleEditForm.builder()
                .month(AUGUST_2026)
                .build();

        ScheduleMonth mapped = ScheduleMonth.builder()
                .month(AUGUST_2026)
                .build();

        when(userService.getScheduleMonth(any(ScheduleEditForm.class))).thenReturn(mapped);

        assertThatThrownBy(() -> service.validateSchedule(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("calculation profile");
    }

    @Test
    void validateSchedule_shouldReturnResultImmediately_whenScheduleDaysAreNull() {
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        ScheduleMonth month = ScheduleMonth.builder().month(AUGUST_2026).calculationProfile(calculationProfile(10, 2)).days(null).build();

        when(userService.getScheduleMonth(form)).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(AUGUST_2026.atDay(1), 2)).thenReturn(Map.of());
        when(userStatisticService.returnScheduleScoreAsString(month, 6)).thenReturn("0/186");

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.getScheduleMonth()).isSameAs(month);
        assertThat(result.isAllUsersExist()).isTrue();
        assertThat(result.isErrorsExist()).isFalse();
        assertThat(result.getScheduleScore()).isEqualTo("0/186");
    }

    @Test
    void validateSchedule_shouldSkipNullDaysAssignmentsAndUsersWithoutIds() {
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        ScheduleMonth month = mock(ScheduleMonth.class);
        ScheduleDay dayWithoutAssignments = mock(ScheduleDay.class);
        ScheduleDay validationDay = mock(ScheduleDay.class);
        ShiftAssignment assignmentWithoutUser = mock(ShiftAssignment.class);
        ShiftAssignment assignmentWithUserWithoutId = mock(ShiftAssignment.class);
        UserCalculationData userWithoutId = mock(UserCalculationData.class);

        List<ScheduleDay> initialDays = new java.util.ArrayList<>();
        initialDays.add(null);
        initialDays.add(dayWithoutAssignments);

        List<ShiftAssignment> validationAssignments = new java.util.ArrayList<>();
        validationAssignments.add(null);
        validationAssignments.add(assignmentWithoutUser);
        validationAssignments.add(assignmentWithUserWithoutId);

        when(month.getCalculationProfile()).thenReturn(calculationProfile(10, 2));
        when(month.getMonth()).thenReturn(AUGUST_2026);
        when(month.getDays()).thenReturn(initialDays, List.of(validationDay), initialDays);
        when(dayWithoutAssignments.getAssignments()).thenReturn(null);
        when(validationDay.getAssignments()).thenReturn(validationAssignments);
        when(assignmentWithoutUser.getUserCalculationData()).thenReturn(null);
        when(assignmentWithUserWithoutId.getUserCalculationData()).thenReturn(userWithoutId);
        when(userWithoutId.userId()).thenReturn(null);
        when(userService.getScheduleMonth(form)).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(AUGUST_2026.atDay(1), 2)).thenReturn(Map.of());
        stubEmptyStatistics();

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isErrorsExist()).isFalse();
        assertThat(result.isAllUsersExist()).isTrue();
    }

    @Test
    void validateSchedule_shouldMarkUserNoRequest_whenOverrideIsDisabled() {
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        ScheduleDay day = mock(ScheduleDay.class);
        ShiftAssignment assignment = mock(ShiftAssignment.class);
        UserCalculationData user = new UserCalculationData(1L, "Freddie", "freddie", null, Set.of(1), Set.of(), Map.of(), Set.of(), false);

        month.setCalculationProfile(calculationProfile(10, 2));
        month.setDays(List.of(day));

        when(day.getDate()).thenReturn(AUGUST_2026.atDay(5));
        when(day.getAssignments()).thenReturn(List.of(assignment));
        when(assignment.getUserCalculationData()).thenReturn(user);
        when(assignment.getShiftType()).thenReturn(1);
        when(userService.getScheduleMonth(form)).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(AUGUST_2026.atDay(1), 2)).thenReturn(Map.of());
        stubEmptyStatistics();

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isErrorsExist()).isTrue();
        assertThat(result.isUserNoRequest()).isTrue();
        assertThat(result.getRedFieldsByShiftType().get(1)).contains(5);
    }

    @Test
    void validateSchedule_shouldIgnoreUserWithoutRequest_whenRequestOverrideIsEnabled() {
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        ScheduleDay day = mock(ScheduleDay.class);
        ShiftAssignment assignment = mock(ShiftAssignment.class);
        UserCalculationData user = new UserCalculationData(1L, "Freddie", "freddie", null, Set.of(1), Set.of(), Map.of(), Set.of(), false);

        month.setCalculationProfile(calculationProfile(10, 2));
        month.setOverrideHasShiftRequest(true);
        month.setDays(List.of(day));

        //when(day.getDate()).thenReturn(AUGUST_2026.atDay(5));
        when(day.getAssignments()).thenReturn(List.of(assignment));
        when(assignment.getUserCalculationData()).thenReturn(user);
        when(assignment.getShiftType()).thenReturn(1);
        when(userService.getScheduleMonth(form)).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(AUGUST_2026.atDay(1), 2)).thenReturn(Map.of());
        stubEmptyStatistics();

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isErrorsExist()).isFalse();
        assertThat(result.isUserNoRequest()).isFalse();
    }

    @Test
    void validateSchedule_shouldMarkAllApplicableWeekdayErrors_whenRulesFailAndOverridesAreDisabled() {
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        ScheduleDay day = mock(ScheduleDay.class);
        ShiftAssignment assignment = mock(ShiftAssignment.class);
        UserCalculationData user = new UserCalculationData(1L, "Freddie", "freddie", null, Set.of(1), Set.of(AUGUST_2026.atDay(5)), Map.of(), Set.of(), true);

        month.setCalculationProfile(calculationProfile(1, 2));
        month.setDays(List.of(day));

        when(day.getDate()).thenReturn(AUGUST_2026.atDay(5));
        when(day.getAssignments()).thenReturn(List.of(assignment));
        when(day.isWeekendOrHoliday()).thenReturn(false);
        when(assignment.getUserCalculationData()).thenReturn(user);
        when(assignment.getShiftType()).thenReturn(1);
        when(userService.getScheduleMonth(form)).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(AUGUST_2026.atDay(1), 2)).thenReturn(Map.of());
        when(scheduleRuleService.isValidWithinTotalShiftLimit(anyInt(), any(UserCalculationData.class), any(CalculationCounters.class))).thenReturn(false);
        when(scheduleRuleService.isValidWithinRequestedWeekdayLimit(any(UserCalculationData.class), anyInt(), any(CalculationCounters.class))).thenReturn(false);
        when(scheduleRuleService.respectsMinimalGap(any(), anyInt(), any(UserCalculationData.class), any(ScheduleMonth.class), anyInt())).thenReturn(false);
        when(scheduleRuleService.isNotRejectedByUser(any(), any())).thenReturn(false);
        when(scheduleRuleService.respectsPreviousMonthGap(any(), anyInt(), any(), any(UserCalculationData.class))).thenReturn(false);
        stubEmptyStatistics();

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isErrorsExist()).isTrue();
        assertThat(result.isUserShiftCap()).isTrue();
        assertThat(result.isUserIndividualShiftCap()).isTrue();
        assertThat(result.isUserWeekendCap()).isFalse();
        assertThat(result.isUserCrossCheck()).isTrue();
        assertThat(result.isUserDatesNo()).isTrue();
        assertThat(result.isPreviousMonthCheckFailed()).isTrue();
        assertThat(result.getRedFieldsByShiftType().get(1)).contains(5);
    }

    @Test
    void validateSchedule_shouldMarkWeekendCapError_whenWeekendLimitFails() {
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        ScheduleDay day = mock(ScheduleDay.class);
        ShiftAssignment assignment = mock(ShiftAssignment.class);
        UserCalculationData user = new UserCalculationData(1L, "Freddie", "freddie", null, Set.of(1), Set.of(), Map.of(), Set.of(), true);

        month.setCalculationProfile(calculationProfile(10, 2));
        month.setDays(List.of(day));

        when(day.getDate()).thenReturn(AUGUST_2026.atDay(8));
        when(day.getAssignments()).thenReturn(List.of(assignment));
        when(day.isWeekendOrHoliday()).thenReturn(true);
        when(assignment.getUserCalculationData()).thenReturn(user);
        when(assignment.getShiftType()).thenReturn(1);
        when(userService.getScheduleMonth(form)).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(AUGUST_2026.atDay(1), 2)).thenReturn(Map.of());
        when(scheduleRuleService.isValidWithinTotalShiftLimit(anyInt(), any(UserCalculationData.class), any(CalculationCounters.class))).thenReturn(true);
        when(scheduleRuleService.isValidWithinRequestedWeekendLimit(any(UserCalculationData.class), anyInt(), any(CalculationCounters.class))).thenReturn(false);
        when(scheduleRuleService.respectsMinimalGap(any(), anyInt(), any(UserCalculationData.class), any(ScheduleMonth.class), anyInt())).thenReturn(true);
        when(scheduleRuleService.isNotRejectedByUser(any(), any())).thenReturn(true);
        when(scheduleRuleService.respectsPreviousMonthGap(any(), anyInt(), any(), any(UserCalculationData.class))).thenReturn(true);
        stubEmptyStatistics();

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isErrorsExist()).isTrue();
        assertThat(result.isUserWeekendCap()).isTrue();
        assertThat(result.isUserIndividualShiftCap()).isFalse();
        assertThat(result.getRedFieldsByShiftType().get(1)).contains(8);
    }

    @Test
    void validateSchedule_shouldSuppressRuleErrors_whenAllRelevantOverridesAreEnabled() {
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        ScheduleDay day = mock(ScheduleDay.class);
        ShiftAssignment assignment = mock(ShiftAssignment.class);
        UserCalculationData user = new UserCalculationData(1L, "Freddie", "freddie", null, Set.of(1), Set.of(AUGUST_2026.atDay(5)), Map.of(), Set.of(), true);

        month.setCalculationProfile(calculationProfile(1, 2));
        month.setOverrideShiftCountCap(true);
        month.setOverrideUserShiftRequestExceptNoDates(true);
        month.setOverrideUserShiftRequestAll(true);
        month.setOverrideConflictingDates(true);
        month.setOverridePreviousMonthValid(true);
        month.setDays(List.of(day));

        when(day.getDate()).thenReturn(AUGUST_2026.atDay(5));
        when(day.getAssignments()).thenReturn(List.of(assignment));
        when(day.isWeekendOrHoliday()).thenReturn(false);
        when(assignment.getUserCalculationData()).thenReturn(user);
        when(assignment.getShiftType()).thenReturn(1);
        when(userService.getScheduleMonth(form)).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(AUGUST_2026.atDay(1), 2)).thenReturn(Map.of());
        when(scheduleRuleService.isValidWithinTotalShiftLimit(anyInt(), any(UserCalculationData.class), any(CalculationCounters.class))).thenReturn(false);
        when(scheduleRuleService.isValidWithinRequestedWeekdayLimit(any(UserCalculationData.class), anyInt(), any(CalculationCounters.class))).thenReturn(false);
        when(scheduleRuleService.respectsMinimalGap(any(), anyInt(), any(UserCalculationData.class), any(ScheduleMonth.class), anyInt())).thenReturn(false);
        when(scheduleRuleService.isNotRejectedByUser(any(), any())).thenReturn(false);
        when(scheduleRuleService.respectsPreviousMonthGap(any(), anyInt(), any(), any(UserCalculationData.class))).thenReturn(false);
        stubEmptyStatistics();

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isErrorsExist()).isFalse();
        assertThat(result.isUserShiftCap()).isFalse();
        assertThat(result.isUserIndividualShiftCap()).isFalse();
        assertThat(result.isUserCrossCheck()).isFalse();
        assertThat(result.isUserDatesNo()).isFalse();
        assertThat(result.isPreviousMonthCheckFailed()).isFalse();
    }

    @Test
    void validateSchedule_shouldSkipRequestedCountValidation_forForceFillShiftType() {
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        CalculationProfileForm profile = CalculationProfileForm.builder().calculationMonth(AUGUST_2026).shiftCountCap(10).gapBetweenShifts(2).forceFillShiftTypes(List.of(1)).build();
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        ScheduleDay day = mock(ScheduleDay.class);
        ShiftAssignment assignment = mock(ShiftAssignment.class);
        UserCalculationData user = new UserCalculationData(1L, "Freddie", "freddie", null, Set.of(1), Set.of(), Map.of(), Set.of(), true);

        month.setCalculationProfile(profile);
        month.setDays(List.of(day));

        when(day.getDate()).thenReturn(AUGUST_2026.atDay(5));
        when(day.getAssignments()).thenReturn(List.of(assignment));
        when(day.isWeekendOrHoliday()).thenReturn(false);
        when(assignment.getUserCalculationData()).thenReturn(user);
        when(assignment.getShiftType()).thenReturn(1);
        when(userService.getScheduleMonth(form)).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(AUGUST_2026.atDay(1), 2)).thenReturn(Map.of());
        when(scheduleRuleService.isValidWithinTotalShiftLimit(anyInt(), any(UserCalculationData.class), any(CalculationCounters.class))).thenReturn(true);
        when(scheduleRuleService.isValidWithinRequestedWeekdayLimit(any(UserCalculationData.class), anyInt(), any(CalculationCounters.class))).thenReturn(false);
        when(scheduleRuleService.respectsMinimalGap(any(), anyInt(), any(UserCalculationData.class), any(ScheduleMonth.class), anyInt())).thenReturn(true);
        when(scheduleRuleService.isNotRejectedByUser(any(), any())).thenReturn(true);
        when(scheduleRuleService.respectsPreviousMonthGap(any(), anyInt(), any(), any(UserCalculationData.class))).thenReturn(true);
        stubEmptyStatistics();

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isErrorsExist()).isFalse();
        assertThat(result.isUserIndividualShiftCap()).isFalse();
    }

    @Test
    void validateSchedule_shouldValidateNormally_whenForceFillShiftTypesAreNullAndAllRulesPass() {
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        CalculationProfileForm profile = CalculationProfileForm.builder().calculationMonth(AUGUST_2026).shiftCountCap(10).gapBetweenShifts(2).forceFillShiftTypes(null).build();
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        ScheduleDay day = mock(ScheduleDay.class);
        ShiftAssignment assignment = mock(ShiftAssignment.class);
        UserCalculationData user = new UserCalculationData(1L, "Freddie", "freddie", null, Set.of(1), Set.of(), Map.of(), Set.of(), true);

        month.setCalculationProfile(profile);
        month.setDays(List.of(day));

        when(day.getDate()).thenReturn(AUGUST_2026.atDay(5));
        when(day.getAssignments()).thenReturn(List.of(assignment));
        when(day.isWeekendOrHoliday()).thenReturn(false);
        when(assignment.getUserCalculationData()).thenReturn(user);
        when(assignment.getShiftType()).thenReturn(1);
        when(userService.getScheduleMonth(form)).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(AUGUST_2026.atDay(1), 2)).thenReturn(Map.of());
        when(scheduleRuleService.isValidWithinTotalShiftLimit(anyInt(), any(UserCalculationData.class), any(CalculationCounters.class))).thenReturn(true);
        when(scheduleRuleService.isValidWithinRequestedWeekdayLimit(any(UserCalculationData.class), anyInt(), any(CalculationCounters.class))).thenReturn(true);
        when(scheduleRuleService.respectsMinimalGap(any(), anyInt(), any(UserCalculationData.class), any(ScheduleMonth.class), anyInt())).thenReturn(true);
        when(scheduleRuleService.isNotRejectedByUser(any(), any())).thenReturn(true);
        when(scheduleRuleService.respectsPreviousMonthGap(any(), anyInt(), any(), any(UserCalculationData.class))).thenReturn(true);
        stubEmptyStatistics();

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isErrorsExist()).isFalse();
        assertThat(result.isUserShiftCap()).isFalse();
        assertThat(result.isUserIndividualShiftCap()).isFalse();
        assertThat(result.isUserWeekendCap()).isFalse();
        assertThat(result.isUserCrossCheck()).isFalse();
        assertThat(result.isUserDatesNo()).isFalse();
        assertThat(result.isPreviousMonthCheckFailed()).isFalse();
    }

    private CalculationProfileForm calculationProfile(int shiftCountCap, int minimalGap) {
        return CalculationProfileForm.builder()
                .calculationMonth(AUGUST_2026)
                .shiftCountCap(shiftCountCap)
                .gapBetweenShifts(minimalGap)
                .forceFillShiftTypes(List.of())
                .build();
    }

    private void stubEmptyStatistics() {
        when(userStatisticService.returnQuickUserStats(
                any(ScheduleMonth.class),
                anyInt(),
                any(CalculationCounters.class)
        )).thenReturn(Map.of());

        when(userStatisticService.returnNoShiftAssignedUserStatMap(
                any(ScheduleMonth.class),
                any(CalculationCounters.class)
        )).thenReturn(Map.of());

        when(userStatisticService.returnFullUserStats(
                any(ScheduleMonth.class),
                any(CalculationCounters.class)
        )).thenReturn(Map.of());

        when(userStatisticService.returnScheduleScoreAsString(
                any(ScheduleMonth.class),
                anyInt()
        )).thenReturn("0/186");
    }
}
