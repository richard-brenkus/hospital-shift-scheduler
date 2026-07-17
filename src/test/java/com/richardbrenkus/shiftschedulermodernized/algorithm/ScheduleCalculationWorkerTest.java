package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.algorithm.record.*;
import com.richardbrenkus.shiftschedulermodernized.service.ScheduleGenerationEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleCalculationWorkerTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private ScheduleGenerationEngine scheduleGenerationEngine;

    @InjectMocks
    private ScheduleCalculationWorker worker;

    @Test
    void shouldReturnBestCandidateAcrossAttempts() {
        CalculationInput input = input(AUGUST_2026, List.of());

        when(scheduleGenerationEngine.assignForceFillShifts(
                any(), anyList(), same(input), any(), any()
        )).thenReturn(1, 4, 2);

        when(scheduleGenerationEngine.assignRegularShifts(
                any(), anyList(), same(input), any(), any()
        )).thenReturn(1, 2, 1);

        ScheduleCandidate result =
                worker.calculateBestCandidate(input, 3, 2);

        assertThat(result.workerIndex()).isEqualTo(2);
        assertThat(result.attemptIndex()).isEqualTo(1);
        assertThat(result.hitCounter()).isEqualTo(6);

        verify(scheduleGenerationEngine, times(3))
                .assignForceFillShifts(
                        any(), anyList(), same(input), any(), any()
                );
        verify(scheduleGenerationEngine, times(3))
                .assignRegularShifts(
                        any(), anyList(), same(input), any(), any()
                );
    }

    @Test
    void shouldCreateAllDaysAndMarkWeekendsAndProvidedHolidays() {
        LocalDate extraHoliday = LocalDate.of(2026, 8, 5);
        CalculationInput input = input(
                AUGUST_2026,
                List.of(extraHoliday)
        );

        when(scheduleGenerationEngine.assignForceFillShifts(
                any(), anyList(), same(input), any(), any()
        )).thenReturn(0);
        when(scheduleGenerationEngine.assignRegularShifts(
                any(), anyList(), same(input), any(), any()
        )).thenReturn(0);

        CalculatedScheduleMonth month =
                worker.calculateBestCandidate(input, 1, 0)
                        .scheduleMonth();

        assertThat(month.getDays())
                .hasSize(AUGUST_2026.lengthOfMonth());
        assertThat(day(month, 1).isWeekendOrHoliday()).isTrue();
        assertThat(day(month, 2).isWeekendOrHoliday()).isTrue();
        assertThat(day(month, 3).isWeekendOrHoliday()).isFalse();
        assertThat(day(month, 5).isWeekendOrHoliday()).isTrue();
    }

    @Test
    void shouldPassEveryDayOfMonthExactlyOnceToEngine() {
        CalculationInput input = input(AUGUST_2026, List.of());

        when(scheduleGenerationEngine.assignForceFillShifts(
                any(), anyList(), same(input), any(), any()
        )).thenReturn(0);
        when(scheduleGenerationEngine.assignRegularShifts(
                any(), anyList(), same(input), any(), any()
        )).thenReturn(0);

        worker.calculateBestCandidate(input, 1, 0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(scheduleGenerationEngine).assignForceFillShifts(
                any(),
                captor.capture(),
                same(input),
                any(),
                any(Random.class)
        );

        assertThat(captor.getValue())
                .containsExactlyInAnyOrderElementsOf(
                        java.util.stream.IntStream.rangeClosed(
                                1,
                                AUGUST_2026.lengthOfMonth()
                        ).boxed().toList()
                );
    }

    @Test
    void shouldRejectNonPositiveAttemptCount() {
        assertThatThrownBy(() ->
                worker.calculateBestCandidate(
                        input(AUGUST_2026, List.of()),
                        0,
                        0
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attempts");

        assertThatThrownBy(() ->
                worker.calculateBestCandidate(
                        input(AUGUST_2026, List.of()),
                        -1,
                        0
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attempts");
    }

    private CalculatedScheduleDay day(
            CalculatedScheduleMonth month,
            int dayOfMonth
    ) {
        return month.getDays().stream()
                .filter(day ->
                        day.getDate().getDayOfMonth() == dayOfMonth
                )
                .findFirst()
                .orElseThrow();
    }

    private CalculationInput input(
            YearMonth month,
            List<LocalDate> holidays
    ) {
        return new CalculationInput(
                month,
                List.of(),
                List.of(1),
                List.of(1),
                List.of(1),
                holidays,
                new CalculationProfile(
                        10, 5, false, List.of()
                )
        );
    }
}
