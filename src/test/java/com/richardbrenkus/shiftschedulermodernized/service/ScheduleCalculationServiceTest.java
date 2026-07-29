package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculatedScheduleConverter;
import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationInputLoader;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ParallelScheduleCalculationService;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculationInput;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculationProfile;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ScheduleCandidate;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleCalculationServiceTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private CalculationInputLoader calculationInputLoader;

    @Mock
    private ParallelScheduleCalculationService parallelService;

    @Mock
    private CalculatedScheduleConverter calculatedScheduleConverter;

    @Mock
    private ActivityPublisher activityPublisher;

    @InjectMocks
    private ScheduleCalculationService service;

    @Test
    void shouldLoadInputCalculateBestCandidateAndConvertWinner() {
        CalculationProfileForm form = CalculationProfileForm.builder().calculationMonth(AUGUST_2026).shiftCountCap(10).gapBetweenShifts(5).sortByDatesAmount(false).forceFillShiftTypes(List.of(1, 2)).build();

        CalculationInput input = new CalculationInput(AUGUST_2026, List.of(), List.of(1, 2, 3), List.of(1, 2, 3), List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), List.of(), new CalculationProfile(10, 5, false, List.of(1, 2)));

        CalculatedScheduleMonth calculatedMonth = CalculatedScheduleMonth.builder().month(AUGUST_2026).hitCounter(42).days(new ArrayList<>()).build();

        ScheduleCandidate bestCandidate = ScheduleCandidate.from(calculatedMonth, 1, 7, 12345L);

        ScheduleMonth convertedSchedule = ScheduleMonth.builder().month(AUGUST_2026).hitCounter(42).days(new ArrayList<>()).calculationProfile(form).build();

        when(calculationInputLoader.load(form)).thenReturn(input);
        when(parallelService.calculateBestSchedule(input)).thenReturn(bestCandidate);
        when(calculatedScheduleConverter.toLegacyScheduleMonth(bestCandidate, form)).thenReturn(convertedSchedule);

        ScheduleMonth result = service.calculateSchedule(form);

        assertThat(result).isSameAs(convertedSchedule);
        assertThat(result.getHitCounter()).isEqualTo(42);

        InOrder inOrder = inOrder(calculationInputLoader, parallelService, calculatedScheduleConverter);

        inOrder.verify(calculationInputLoader).load(form);
        inOrder.verify(parallelService).calculateBestSchedule(input);
        inOrder.verify(calculatedScheduleConverter).toLegacyScheduleMonth(bestCandidate, form);

        verifyNoMoreInteractions(calculationInputLoader, parallelService, calculatedScheduleConverter);
    }

    @Test
    void shouldPassOriginalFormToInputLoaderAndConverter() {
        CalculationProfileForm form = CalculationProfileForm.builder().calculationMonth(AUGUST_2026).shiftCountCap(3).gapBetweenShifts(2).sortByDatesAmount(true).forceFillShiftTypes(List.of(6)).build();

        CalculationInput input = new CalculationInput(AUGUST_2026, List.of(), List.of(1, 2, 3, 4, 5, 6), List.of(6, 1, 2, 3, 4, 5), List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), List.of(), new CalculationProfile(3, 2, true, List.of(6)));

        CalculatedScheduleMonth calculatedMonth = CalculatedScheduleMonth.builder().month(AUGUST_2026).hitCounter(0).days(new ArrayList<>()).build();

        ScheduleCandidate candidate = ScheduleCandidate.from(calculatedMonth, 0, 0, 999L);

        ScheduleMonth converted = ScheduleMonth.builder().month(AUGUST_2026).hitCounter(0).days(new ArrayList<>()).calculationProfile(form).build();

        when(calculationInputLoader.load(form)).thenReturn(input);
        when(parallelService.calculateBestSchedule(input)).thenReturn(candidate);
        when(calculatedScheduleConverter.toLegacyScheduleMonth(candidate, form)).thenReturn(converted);

        service.calculateSchedule(form);

        verify(calculationInputLoader).load(form);
        verify(calculatedScheduleConverter).toLegacyScheduleMonth(candidate, form);
    }
}