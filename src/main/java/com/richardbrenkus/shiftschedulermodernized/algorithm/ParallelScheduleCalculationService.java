package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculationInput;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ScheduleCandidate;
import com.richardbrenkus.shiftschedulermodernized.config.ScheduleCalculationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class ParallelScheduleCalculationService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ParallelScheduleCalculationService.class
            );

    private final ScheduleCalculationWorker worker;
    private final Executor scheduleCalculationExecutor;
    private final ScheduleCalculationProperties properties;

    public ParallelScheduleCalculationService(
            ScheduleCalculationWorker worker,
            @Qualifier("scheduleCalculationExecutor")
            Executor scheduleCalculationExecutor,
            ScheduleCalculationProperties properties
    ) {
        this.worker = worker;
        this.scheduleCalculationExecutor =
                scheduleCalculationExecutor;
        this.properties = properties;
    }

    public ScheduleCandidate calculateBestSchedule(CalculationInput input) {
        validateProperties();

        Duration timeout = properties.timeout();

        List<CompletableFuture<ScheduleCandidate>> futures = IntStream.range(0, properties.numberOfThreads())
                        .mapToObj(workerNumber -> {
                            log.info("Submitting schedule worker {} from thread {}", workerNumber, Thread.currentThread().getName());

                            return CompletableFuture.supplyAsync(() -> worker.calculateBestCandidate(input, properties.attemptsPerThread(), workerNumber), scheduleCalculationExecutor)
                                    .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                                    .exceptionally(exception -> {
                                        Throwable cause = unwrapException(exception);
                                        log.error("Schedule worker {} failed or timed out", workerNumber, cause);

                                        return null;
                                    });
                        })
                        .toList();

        /*
         * Because every worker future handles its own exception and returns
         * null on failure, join() should normally no longer throw because of
         * an individual worker failure.
         */
        List<ScheduleCandidate> successfulCandidates;

        try {
            successfulCandidates = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (CompletionException exception) {
            /*
             * Defensive fallback for exceptional situations not handled by
             * the per-future exceptionally() stage.
             */
            futures.forEach(future -> future.cancel(true));

            Throwable cause = unwrapException(exception);

            throw new IllegalStateException("Parallel schedule calculation failed unexpectedly.", cause);
        }

        if (successfulCandidates.isEmpty()) {
            throw new IllegalStateException("All schedule calculation workers failed.");
        }

        ScheduleCandidate winner = successfulCandidates.stream()
                        .max(ScheduleCandidateComparators.BY_QUALITY)
                        .orElseThrow(() -> new IllegalStateException("No valid schedule candidate was produced."));

        log.info("Winning candidate: worker={}, attempt={}, hitCounter={}", winner.workerIndex(), winner.attemptIndex(), winner.hitCounter());

        return winner;
    }

    private Throwable unwrapException(Throwable exception) {
        if (exception instanceof CompletionException && exception.getCause() != null) {
            return exception.getCause();
        }

        return exception;
    }

    private void validateProperties() {
        if (properties.numberOfThreads() <= 0) {
            throw new IllegalStateException("numberOfThreads must be positive");
        }

        if (properties.attemptsPerThread() <= 0) {
            throw new IllegalStateException("attemptsPerThread must be positive");
        }

        if (properties.timeout() == null || properties.timeout().isZero() || properties.timeout().isNegative()) {
            throw new IllegalStateException("timeout must be positive");
        }
    }
}