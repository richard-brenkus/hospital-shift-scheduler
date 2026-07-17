package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.algorithm.record.*;
import com.richardbrenkus.shiftschedulermodernized.config.ScheduleCalculationProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParallelScheduleCalculationServiceTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);
    private final Executor directExecutor = Runnable::run;

    @Test
    void shouldSelectHighestScoringCandidateAcrossWorkers() {
        ScheduleCalculationWorker worker = mock(ScheduleCalculationWorker.class);
        CalculationInput input = input();

        when(worker.calculateBestCandidate(input, 100, 0))
                .thenReturn(candidate(140, 0, 12));
        when(worker.calculateBestCandidate(input, 100, 1))
                .thenReturn(candidate(145, 1, 30));
        when(worker.calculateBestCandidate(input, 100, 2))
                .thenReturn(candidate(143, 2, 7));
        when(worker.calculateBestCandidate(input, 100, 3))
                .thenReturn(candidate(144, 3, 9));

        ParallelScheduleCalculationService service =
                new ParallelScheduleCalculationService(
                        worker,
                        directExecutor,
                        new ScheduleCalculationProperties(
                                4, 100, Duration.ofSeconds(5)
                        )
                );

        ScheduleCandidate result = service.calculateBestSchedule(input);

        assertThat(result.hitCounter()).isEqualTo(145);
        assertThat(result.workerIndex()).isEqualTo(1);
        assertThat(result.attemptIndex()).isEqualTo(30);

        verify(worker, times(4))
                .calculateBestCandidate(eq(input), eq(100), anyInt());
    }

    @Test
    void shouldPreferLowerWorkerIndexWhenScoresTie() {
        ScheduleCalculationWorker worker = mock(ScheduleCalculationWorker.class);
        CalculationInput input = input();

        when(worker.calculateBestCandidate(input, 10, 0))
                .thenReturn(candidate(145, 0, 9));
        when(worker.calculateBestCandidate(input, 10, 1))
                .thenReturn(candidate(145, 1, 1));

        ScheduleCandidate result =
                new ParallelScheduleCalculationService(
                        worker,
                        directExecutor,
                        new ScheduleCalculationProperties(
                                2, 10, Duration.ofSeconds(5)
                        )
                ).calculateBestSchedule(input);

        assertThat(result.workerIndex()).isZero();
    }

    @Test
    void shouldWrapWorkerFailure() {
        ScheduleCalculationWorker worker = mock(ScheduleCalculationWorker.class);
        CalculationInput input = input();

        when(worker.calculateBestCandidate(input, 10, 0))
                .thenThrow(new IllegalArgumentException("worker failed"));

        ParallelScheduleCalculationService service =
                new ParallelScheduleCalculationService(
                        worker,
                        directExecutor,
                        new ScheduleCalculationProperties(
                                1, 10, Duration.ofSeconds(5)
                        )
                );

        assertThatThrownBy(() -> service.calculateBestSchedule(input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("All schedule calculation workers failed.");
                //.hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidConfigurationValues() {
        assertInvalid(
                new ScheduleCalculationProperties(
                        0, 10, Duration.ofSeconds(1)
                ),
                "numberOfThreads"
        );

        assertInvalid(
                new ScheduleCalculationProperties(
                        2, 0, Duration.ofSeconds(1)
                ),
                "attemptsPerThread"
        );

        assertInvalid(
                new ScheduleCalculationProperties(2, 10, null),
                "timeout"
        );

        assertInvalid(
                new ScheduleCalculationProperties(
                        2, 10, Duration.ZERO
                ),
                "timeout"
        );

        assertInvalid(
                new ScheduleCalculationProperties(
                        2, 10, Duration.ofSeconds(-1)
                ),
                "timeout"
        );
    }

    @Test
    void shouldIgnoreFailedWorkerIfOtherWorkersSucceed() {
        ScheduleCalculationWorker worker =
                mock(ScheduleCalculationWorker.class);

        CalculationInput input = input();

        when(worker.calculateBestCandidate(input, 10, 0))
                .thenThrow(new IllegalStateException(
                        "Worker 0 failed"
                ));

        when(worker.calculateBestCandidate(input, 10, 1))
                .thenReturn(candidate(
                        145,
                        1,
                        4
                ));

        ParallelScheduleCalculationService service =
                new ParallelScheduleCalculationService(
                        worker,
                        directExecutor,
                        new ScheduleCalculationProperties(
                                2,
                                10,
                                Duration.ofSeconds(5)
                        )
                );

        ScheduleCandidate result =
                service.calculateBestSchedule(input);

        assertThat(result.hitCounter()).isEqualTo(145);
        assertThat(result.workerIndex()).isEqualTo(1);
        assertThat(result.attemptIndex()).isEqualTo(4);

        verify(worker).calculateBestCandidate(
                input,
                10,
                0
        );

        verify(worker).calculateBestCandidate(
                input,
                10,
                1
        );
    }

    @Test
    void shouldThrowMeaningfulErrorWhenAllWorkersFail() {
        ScheduleCalculationWorker worker =
                mock(ScheduleCalculationWorker.class);

        CalculationInput input = input();

        when(worker.calculateBestCandidate(
                eq(input),
                eq(10),
                anyInt()
        )).thenThrow(
                new IllegalStateException("Worker failed")
        );

        ParallelScheduleCalculationService service =
                new ParallelScheduleCalculationService(
                        worker,
                        directExecutor,
                        new ScheduleCalculationProperties(
                                3,
                                10,
                                Duration.ofSeconds(5)
                        )
                );

        assertThatThrownBy(() ->
                service.calculateBestSchedule(input)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "All schedule calculation workers failed."
                );

        verify(worker, times(3))
                .calculateBestCandidate(
                        eq(input),
                        eq(10),
                        anyInt()
                );
    }

    @Test
    void shouldIgnoreTimedOutWorkerIfOtherWorkersSucceed() {

        ScheduleCalculationWorker worker =
                mock(ScheduleCalculationWorker.class);

        CalculationInput input = input();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            when(worker.calculateBestCandidate(
                    input,
                    10,
                    0
            )).thenAnswer(invocation -> {
                Thread.sleep(500);
                return candidate(
                        200,
                        0,
                        1
                );
            });

            when(worker.calculateBestCandidate(
                    input,
                    10,
                    1
            )).thenReturn(candidate(
                    145,
                    1,
                    5
            ));

            ParallelScheduleCalculationService service =
                    new ParallelScheduleCalculationService(
                            worker,
                            executor,
                            new ScheduleCalculationProperties(
                                    2,
                                    10,
                                    Duration.ofMillis(100)
                            )
                    );

            ScheduleCandidate result =
                    service.calculateBestSchedule(input);

            assertThat(result.hitCounter()).isEqualTo(145);
            assertThat(result.workerIndex()).isEqualTo(1);
            assertThat(result.attemptIndex()).isEqualTo(5);

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldSelectWinnerAmongRemainingWorkersWhenOneFails() {
        ScheduleCalculationWorker worker =
                mock(ScheduleCalculationWorker.class);

        CalculationInput input = input();

        when(worker.calculateBestCandidate(input, 20, 0))
                .thenReturn(candidate(
                        141,
                        0,
                        3
                ));

        when(worker.calculateBestCandidate(input, 20, 1))
                .thenThrow(
                        new IllegalArgumentException(
                                "Worker 1 failed"
                        )
                );

        when(worker.calculateBestCandidate(input, 20, 2))
                .thenReturn(candidate(
                        149,
                        2,
                        7
                ));

        when(worker.calculateBestCandidate(input, 20, 3))
                .thenReturn(candidate(
                        146,
                        3,
                        2
                ));

        ParallelScheduleCalculationService service =
                new ParallelScheduleCalculationService(
                        worker,
                        directExecutor,
                        new ScheduleCalculationProperties(
                                4,
                                20,
                                Duration.ofSeconds(5)
                        )
                );

        ScheduleCandidate result =
                service.calculateBestSchedule(input);

        assertThat(result.hitCounter()).isEqualTo(149);
        assertThat(result.workerIndex()).isEqualTo(2);
        assertThat(result.attemptIndex()).isEqualTo(7);

        verify(worker, times(4))
                .calculateBestCandidate(
                        eq(input),
                        eq(20),
                        anyInt()
                );
    }

    @Test
    void shouldRejectEmptySuccessfulCandidateList() {
        ScheduleCalculationWorker worker =
                mock(ScheduleCalculationWorker.class);

        CalculationInput input = input();

        when(worker.calculateBestCandidate(
                eq(input),
                eq(10),
                anyInt()
        )).thenReturn(null);

        ParallelScheduleCalculationService service =
                new ParallelScheduleCalculationService(
                        worker,
                        directExecutor,
                        new ScheduleCalculationProperties(
                                2,
                                10,
                                Duration.ofSeconds(5)
                        )
                );

        assertThatThrownBy(() ->
                service.calculateBestSchedule(input)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "All schedule calculation workers failed."
                );

        verify(worker, times(2))
                .calculateBestCandidate(
                        eq(input),
                        eq(10),
                        anyInt()
                );
    }

    private void assertInvalid(
            ScheduleCalculationProperties properties,
            String message
    ) {
        ParallelScheduleCalculationService service =
                new ParallelScheduleCalculationService(
                        mock(ScheduleCalculationWorker.class),
                        directExecutor,
                        properties
                );

        assertThatThrownBy(() -> service.calculateBestSchedule(input()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(message);
    }

    private ScheduleCandidate candidate(
            int score,
            int worker,
            int attempt
    ) {
        CalculatedScheduleMonth month =
                CalculatedScheduleMonth.builder()
                        .month(AUGUST_2026)
                        .hitCounter(score)
                        .days(new ArrayList<>())
                        .build();

        return ScheduleCandidate.from(
                month,
                worker,
                attempt,
                1000L + attempt
        );
    }

    private CalculationInput input() {
        return new CalculationInput(
                AUGUST_2026,
                List.of(),
                List.of(1),
                List.of(1),
                List.of(1),
                List.of(),
                new CalculationProfile(
                        10, 5, false, List.of()
                )
        );
    }
}
