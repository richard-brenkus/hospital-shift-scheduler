package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculationInput;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ScheduleCandidate;
import com.richardbrenkus.shiftschedulermodernized.service.ScheduleGenerationEngine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class ScheduleCalculationWorker {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ScheduleCalculationWorker.class
            );

    private final ScheduleGenerationEngine scheduleGenerationEngine;

    public ScheduleCandidate calculateBestCandidate(CalculationInput input, int attempts, int workerNumber) {
        if (attempts <= 0) {
            throw new IllegalArgumentException("attempts must be positive");
        }

        ScheduleCandidate best = null;

        long workerSeed = System.nanoTime() ^ ((long) workerNumber << 32);

        for (int attempt = 0; attempt < attempts; attempt++) {
            /*
             * Allows future.cancel(true), application shutdown and explicit
             * interruption to stop the worker between attempts.
             */
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException("Worker " + workerNumber + " interrupted.");
            }

            long attemptSeed = workerSeed + attempt * 31_415_927L;

            Random random = new Random(attemptSeed);

            CalculatedScheduleMonth candidate = calculateSingleCandidate(input, random);

            ScheduleCandidate result = ScheduleCandidate.from(candidate, workerNumber, attempt, attemptSeed);

            if (best == null || ScheduleCandidateComparators.BY_QUALITY.compare(result, best) > 0) {
                best = result;
            }
        }

        if (best == null) {
            throw new IllegalStateException("Worker " + workerNumber + " produced no valid candidate.");
        }

        log.info("Worker {} completed on {}; best attempt={}, hitCounter={}", workerNumber, Thread.currentThread().getName(), best.attemptIndex(), best.hitCounter());

        return best;
    }

    private CalculatedScheduleMonth calculateSingleCandidate(CalculationInput input, Random random) {
        checkInterrupted();

        CalculatedScheduleMonth scheduleMonth = createEmptyScheduleMonth(input);

        CalculationCounters counters = new CalculationCounters();

        List<Integer> monthDays = IntStream.rangeClosed(1, input.month().lengthOfMonth())
                        .boxed()
                        .collect(Collectors.toCollection(ArrayList::new));

        Collections.shuffle(monthDays, random);

        checkInterrupted();

        int hitCounter = scheduleGenerationEngine.assignForceFillShifts(scheduleMonth, monthDays, input, counters, random);

        checkInterrupted();

        hitCounter += scheduleGenerationEngine.assignRegularShifts(scheduleMonth, monthDays, input, counters, random);

        scheduleMonth.setHitCounter(hitCounter);

        return scheduleMonth;
    }

    private CalculatedScheduleMonth createEmptyScheduleMonth(CalculationInput input) {
        List<CalculatedScheduleDay> days = IntStream.rangeClosed(1, input.month().lengthOfMonth())
                        .mapToObj(input.month()::atDay)
                        .map(date ->
                                CalculatedScheduleDay.builder()
                                        .date(date)
                                        .weekendOrHoliday(
                                                isWeekendOrHoliday(date, input.holidays()))
                                        .assignments(new ArrayList<>())
                                        .build())
                        .toList();

        return CalculatedScheduleMonth.builder()
                .month(input.month())
                .hitCounter(0)
                .days(new ArrayList<>(days))
                .build();
    }

    private boolean isWeekendOrHoliday(LocalDate date, List<LocalDate> holidays) {
        return date.getDayOfWeek().getValue() >= 6 || holidays.contains(date);
    }

    private void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Schedule calculation thread was interrupted.");
        }
    }
}