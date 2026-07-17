package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculationInput;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ScheduleCandidate;
import com.richardbrenkus.shiftschedulermodernized.config.ScheduleCalculationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class ParallelScheduleCalculationService {
    private final ScheduleCalculationWorker worker;
    private final Executor scheduleCalculationExecutor;
    private final ScheduleCalculationProperties properties;

    public ParallelScheduleCalculationService(
            ScheduleCalculationWorker worker,
            @Qualifier("scheduleCalculationExecutor") Executor scheduleCalculationExecutor,
            ScheduleCalculationProperties properties
    ) {
        this.worker = worker;
        this.scheduleCalculationExecutor = scheduleCalculationExecutor;
        this.properties = properties;
    }

    public ScheduleCandidate calculateBestSchedule(CalculationInput input) {
        validateProperties();

        Duration timeout = properties.timeout();

        List<CompletableFuture<ScheduleCandidate>> futures =
                IntStream.range(0, properties.numberOfThreads())
                        .mapToObj(workerNumber -> {

                            System.out.println(
                                    "Submitting worker "
                                            + workerNumber
                                            + " from thread "
                                            + Thread.currentThread().getName()
                            );

                            return CompletableFuture.supplyAsync(
                                            () -> worker.calculateBestCandidate(
                                                    input,
                                                    properties.attemptsPerThread(),
                                                    workerNumber
                                            ),
                                            scheduleCalculationExecutor
                                    )
                                    .orTimeout(
                                            timeout.toMillis(),
                                            TimeUnit.MILLISECONDS
                                    );
                        })
                        .toList();

        try {
            ScheduleCandidate winner = futures.stream()
                    .map(CompletableFuture::join)
                    .max(
                            Comparator.comparingInt(
                                            ScheduleCandidate::hitCounter
                                    )
                                    .thenComparingInt(
                                            candidate -> -candidate.workerIndex()
                                    )
                                    .thenComparingInt(
                                            candidate -> -candidate.attemptIndex()
                                    )
                    )
                    .orElseThrow(
                            () -> new IllegalStateException(
                                    "No schedule candidate was produced"
                            )
                    );

            System.out.println(
                    "Winning candidate: worker="
                            + winner.workerIndex()
                            + ", attempt="
                            + winner.attemptIndex()
                            + ", hitCounter="
                            + winner.hitCounter()
            );
            return winner;

        } catch (CompletionException exception) {
            futures.forEach(future -> future.cancel(true));

            Throwable cause =
                    exception.getCause() == null
                            ? exception
                            : exception.getCause();

            throw new IllegalStateException(
                    "Parallel schedule calculation failed",
                    cause
            );
        }
    }

    private void validateProperties() {
        if (properties.numberOfThreads() <= 0) {
            throw new IllegalStateException("numberOfThreads must be positive");
        }
        if (properties.attemptsPerThread() <= 0) {
            throw new IllegalStateException("attemptsPerThread must be positive");
        }
        if (properties.timeout() == null
                || properties.timeout().isZero()
                || properties.timeout().isNegative()) {
            throw new IllegalStateException("timeout must be positive");
        }
    }
}
