package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleValidationResult;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
