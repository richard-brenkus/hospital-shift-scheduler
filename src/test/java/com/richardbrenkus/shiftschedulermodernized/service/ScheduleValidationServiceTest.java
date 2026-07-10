package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleValidationResult;
import com.richardbrenkus.shiftschedulermodernized.config.ShiftTypeProperties;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ScheduleEditForm;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ScheduleMapper;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleValidationServiceTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private ScheduleMapper scheduleMapper;

    @Mock
    private ScheduleRuleService scheduleRuleService;

    @Mock
    private UserStatisticService userStatisticService;

    private ScheduleValidationService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new ScheduleValidationService(
                new ShiftTypeProperties(6),
                scheduleMapper,
                scheduleRuleService,
                userStatisticService
        );

        when(userStatisticService.returnQuickUserStats(any(), anyInt(), any())).thenReturn(Map.of());
        when(userStatisticService.returnNoShiftAssignedUserStatMap(any(), any())).thenReturn(Map.of());
        when(userStatisticService.returnFullUserStats(any(), any())).thenReturn(Map.of());
        when(userStatisticService.returnScheduleScoreAsString(any(), anyInt())).thenReturn("0/186");
    }

    @Test
    void shouldInitializeValidationAndUserStats_whenGivenScheduleMonth() {
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        month.setCalculationProfile(CalculationProfileForm.builder()
                .calculationMonth(AUGUST_2026).shiftCountCap(10).gapBetweenShifts(0).build());

        ScheduleValidationResult result = service.initializeValidationAndUserStats(month);

        assertThat(result.getScheduleMonth()).isEqualTo(month);
        assertThat(result.getScheduleScore()).isEqualTo("0/186");
        assertThat(result.isErrorsExist()).isFalse();
    }

    @Test
    void shouldReturnEmptyResult_whenValidatingEmptyEditForm() {
        ScheduleEditForm form = ScheduleEditForm.builder().build();
        when(scheduleMapper.toScheduleMonth(any(), any())).thenReturn(null);

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isErrorsExist()).isFalse();
        assertThat(result.isAllUsersExist()).isTrue();
    }

    @Test
    void shouldThrowIllegalArgumentException_whenScheduleHasNoCalculationProfile() {
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        ScheduleMonth mapped = ScheduleMonth.builder().month(AUGUST_2026).build();
        when(scheduleMapper.toScheduleMonth(any(), any())).thenReturn(mapped);

        assertThatThrownBy(() -> service.validateSchedule(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("calculation profile");
    }

    @Test
    void shouldMarkUserNoRequestError_whenAssignedUserHasNoShiftRequestAndOverrideDisabled() {
        User user = TestFixtures.user(1L, "freddie");
        user.setName("Freddie");
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        month.setCalculationProfile(CalculationProfileForm.builder()
                .calculationMonth(AUGUST_2026).shiftCountCap(10).gapBetweenShifts(0).build());
        TestFixtures.assign(month, LocalDate.of(2026, 8, 5), 1, user);
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        when(scheduleMapper.toScheduleMonth(any(), any())).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(any(), anyInt())).thenReturn(Map.of());

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isErrorsExist()).isTrue();
        assertThat(result.isUserNoRequest()).isTrue();
        assertThat(result.getRedFieldsByShiftType().get(1)).contains(5);
    }

    @Test
    void shouldSkipUserNoRequestMarking_whenOverrideHasShiftRequestEnabled() {
        User user = TestFixtures.user(1L, "freddie");
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        month.setCalculationProfile(CalculationProfileForm.builder()
                .calculationMonth(AUGUST_2026).shiftCountCap(10).gapBetweenShifts(0).build());
        month.setOverrideHasShiftRequest(true);
        TestFixtures.assign(month, LocalDate.of(2026, 8, 5), 1, user);
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        when(scheduleMapper.toScheduleMonth(any(), any())).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(any(), anyInt())).thenReturn(Map.of());

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isUserNoRequest()).isFalse();
    }

    @Test
    void shouldMarkShiftCapError_whenTotalShiftLimitExceededAndOverrideDisabled() {
        User user = TestFixtures.user(1L, "freddie");
        user.setName("Freddie");
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 5, 0, true, List.of()));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        month.setCalculationProfile(CalculationProfileForm.builder()
                .calculationMonth(AUGUST_2026).shiftCountCap(1).gapBetweenShifts(0).build());
        TestFixtures.assign(month, LocalDate.of(2026, 8, 5), 1, user);
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        when(scheduleMapper.toScheduleMonth(any(), any())).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(any(), anyInt())).thenReturn(Map.of());
        when(scheduleRuleService.isValidWithinTotalShiftLimit(anyInt(), any(), any())).thenReturn(false);
        when(scheduleRuleService.isValidWithinRequestedWeekdayLimit(any(), anyInt(), any())).thenReturn(true);
        when(scheduleRuleService.isValidWithinRequestedWeekendLimit(any(), anyInt(), any())).thenReturn(true);
        when(scheduleRuleService.respectsMinimalGap(any(), anyInt(), any(), any(), anyInt())).thenReturn(true);
        when(scheduleRuleService.isNotRejectedByUser(any(), any())).thenReturn(true);
        when(scheduleRuleService.respectsPreviousMonthGap(any(), any(), any(), any())).thenReturn(true);

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isUserShiftCap()).isTrue();
    }

    @Test
    void shouldNotMarkShiftCapError_whenOverrideShiftCountCapEnabled() {
        User user = TestFixtures.user(1L, "freddie");
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 5, 0, true, List.of()));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        month.setCalculationProfile(CalculationProfileForm.builder()
                .calculationMonth(AUGUST_2026).shiftCountCap(1).gapBetweenShifts(0).build());
        month.setOverrideShiftCountCap(true);
        TestFixtures.assign(month, LocalDate.of(2026, 8, 5), 1, user);
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        when(scheduleMapper.toScheduleMonth(any(), any())).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(any(), anyInt())).thenReturn(Map.of());
        when(scheduleRuleService.isValidWithinTotalShiftLimit(anyInt(), any(), any())).thenReturn(false);
        when(scheduleRuleService.isValidWithinRequestedWeekdayLimit(any(), anyInt(), any())).thenReturn(true);
        when(scheduleRuleService.isValidWithinRequestedWeekendLimit(any(), anyInt(), any())).thenReturn(true);
        when(scheduleRuleService.respectsMinimalGap(any(), anyInt(), any(), any(), anyInt())).thenReturn(true);
        when(scheduleRuleService.isNotRejectedByUser(any(), any())).thenReturn(true);
        when(scheduleRuleService.respectsPreviousMonthGap(any(), any(), any(), any())).thenReturn(true);

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isUserShiftCap()).isFalse();
    }

    @Test
    void shouldMarkCrossCheckError_whenMinimalGapFailsAndOverrideDisabled() {
        User user = TestFixtures.user(1L, "freddie");
        user.setName("Freddie");
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 5, 0, true, List.of()));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        month.setCalculationProfile(CalculationProfileForm.builder()
                .calculationMonth(AUGUST_2026).shiftCountCap(10).gapBetweenShifts(2).build());
        TestFixtures.assign(month, LocalDate.of(2026, 8, 5), 1, user);
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();
        when(scheduleMapper.toScheduleMonth(any(), any())).thenReturn(month);
        when(scheduleRuleService.loadPreviousStoredScheduleDays(any(), anyInt())).thenReturn(Map.of());
        when(scheduleRuleService.isValidWithinTotalShiftLimit(anyInt(), any(), any())).thenReturn(true);
        when(scheduleRuleService.isValidWithinRequestedWeekdayLimit(any(), anyInt(), any())).thenReturn(true);
        when(scheduleRuleService.isValidWithinRequestedWeekendLimit(any(), anyInt(), any())).thenReturn(true);
        when(scheduleRuleService.respectsMinimalGap(any(), anyInt(), any(), any(), anyInt())).thenReturn(false);
        when(scheduleRuleService.isNotRejectedByUser(any(), any())).thenReturn(true);
        when(scheduleRuleService.respectsPreviousMonthGap(any(), any(), any(), any())).thenReturn(true);

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isUserCrossCheck()).isTrue();
    }
}
