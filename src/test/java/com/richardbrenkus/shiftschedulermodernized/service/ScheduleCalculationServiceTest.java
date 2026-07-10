package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleCalculationServiceTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShiftTypeService shiftTypeService;

    @Mock
    private ScheduleRuleService scheduleRuleService;

    @Mock
    private ScheduleGenerationEngine scheduleGenerationEngine;

    @InjectMocks
    private ScheduleCalculationService service;

    @Test
    void shouldReturnBestScheduleAmongCandidates_whenCalculatingSchedule() {
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 1);
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 5, 0, true, List.of()));
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(shiftTypeService.getShiftTypes()).thenReturn(List.of(1, 2, 3));
        when(scheduleRuleService.loadPreviousStoredScheduleDays(any(), anyInt())).thenReturn(Map.of());

        AtomicInteger call = new AtomicInteger(0);
        // returnCounter oscillates so we can verify max is selected.
        when(scheduleGenerationEngine.assignForceFillShifts(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt(), any(), any()))
                .thenAnswer(invocation -> call.getAndIncrement() % 2 == 0 ? 3 : 5);
        when(scheduleGenerationEngine.assignRegularShifts(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt(), any(), any()))
                .thenReturn(0);

        CalculationProfileForm form = CalculationProfileForm.builder()
                .calculationMonth(AUGUST_2026)
                .shiftCountCap(10)
                .gapBetweenShifts(0)
                .sortByDatesAmount(false)
                .forceFillShiftTypes(List.of())
                .build();

        ScheduleMonth result = service.calculateSchedule(form);

        assertThat(result).isNotNull();
        assertThat(result.getHitCounter()).isEqualTo(5);
        verify(scheduleGenerationEngine, times(100)).assignForceFillShifts(
                any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt(), any(), any());
        verify(scheduleGenerationEngine, times(100)).assignRegularShifts(
                any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void shouldFlagWeekendsInScheduleDays_whenCalculatingSchedule() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(shiftTypeService.getShiftTypes()).thenReturn(List.of(1));
        when(scheduleRuleService.loadPreviousStoredScheduleDays(any(), anyInt())).thenReturn(Map.of());
        when(scheduleGenerationEngine.assignForceFillShifts(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt(), any(), any()))
                .thenReturn(0);
        when(scheduleGenerationEngine.assignRegularShifts(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt(), any(), any()))
                .thenReturn(0);

        CalculationProfileForm form = CalculationProfileForm.builder()
                .calculationMonth(AUGUST_2026)
                .shiftCountCap(10)
                .gapBetweenShifts(0)
                .sortByDatesAmount(false)
                .forceFillShiftTypes(List.of())
                .build();

        ScheduleMonth result = service.calculateSchedule(form);

        // 2026-08-01 is Saturday, 2026-08-08 is a Saturday, 2026-07-05 is Czech holiday (holiday but not in month).
        // 2026-08-15 - Saturday; 2026-08-22 - Saturday; 2026-08-29 - Saturday.
        List<Integer> weekendDayOfMonth = result.getDays()
                .stream()
                .filter(ScheduleDay::isWeekendOrHoliday)
                .map(day -> day.getDate().getDayOfMonth())
                .toList();
        // Aug 2026 weekends: 1,2,8,9,15,16,22,23,29,30. No CZ holidays fall in August.
        assertThat(weekendDayOfMonth).contains(1, 2, 8, 9, 15, 16, 22, 23, 29, 30);
    }

    @Test
    void shouldMarkChristmasAsHoliday_whenCalculatingScheduleForDecember() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(shiftTypeService.getShiftTypes()).thenReturn(List.of(1));
        when(scheduleRuleService.loadPreviousStoredScheduleDays(any(), anyInt())).thenReturn(Map.of());
        when(scheduleGenerationEngine.assignForceFillShifts(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt(), any(), any()))
                .thenReturn(0);
        when(scheduleGenerationEngine.assignRegularShifts(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt(), any(), any()))
                .thenReturn(0);

        CalculationProfileForm form = CalculationProfileForm.builder()
                .calculationMonth(YearMonth.of(2026, 12))
                .shiftCountCap(10)
                .gapBetweenShifts(0)
                .sortByDatesAmount(false)
                .forceFillShiftTypes(List.of())
                .build();

        ScheduleMonth result = service.calculateSchedule(form);

        assertThat(dayFor(result, 24).isWeekendOrHoliday()).isTrue();
        assertThat(dayFor(result, 25).isWeekendOrHoliday()).isTrue();
        assertThat(dayFor(result, 26).isWeekendOrHoliday()).isTrue();
    }

    @Test
    void shouldMarkEasterMondayAsHoliday_whenCalculatingScheduleForAprilOfKnownEasterYear() {
        // For 2026, Good Friday is 2026-04-03 and Easter Monday is 2026-04-06.
        when(userRepository.findAll()).thenReturn(List.of());
        when(shiftTypeService.getShiftTypes()).thenReturn(List.of(1));
        when(scheduleRuleService.loadPreviousStoredScheduleDays(any(), anyInt())).thenReturn(Map.of());
        when(scheduleGenerationEngine.assignForceFillShifts(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt(), any(), any()))
                .thenReturn(0);
        when(scheduleGenerationEngine.assignRegularShifts(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt(), any(), any()))
                .thenReturn(0);

        CalculationProfileForm form = CalculationProfileForm.builder()
                .calculationMonth(YearMonth.of(2026, 4))
                .shiftCountCap(10)
                .gapBetweenShifts(0)
                .sortByDatesAmount(false)
                .forceFillShiftTypes(List.of())
                .build();

        ScheduleMonth result = service.calculateSchedule(form);

        assertThat(dayFor(result, 3).isWeekendOrHoliday()).isTrue();
        assertThat(dayFor(result, 6).isWeekendOrHoliday()).isTrue();
    }

    private ScheduleDay dayFor(ScheduleMonth month, int dayOfMonth) {
        return month.getDays().stream()
                .filter(day -> day.getDate().getDayOfMonth() == dayOfMonth)
                .findFirst()
                .orElseThrow();
    }
}
