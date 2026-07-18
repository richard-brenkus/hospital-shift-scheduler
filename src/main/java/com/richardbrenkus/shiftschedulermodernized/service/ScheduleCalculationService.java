package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculatedScheduleConverter;
import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationInputLoader;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ParallelScheduleCalculationService;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculationInput;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ScheduleCandidate;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.exception.CalculationAlreadyRunningException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class ScheduleCalculationService {

    private final CalculationInputLoader calculationInputLoader;
    private final ParallelScheduleCalculationService parallelService;
    private final CalculatedScheduleConverter calculatedScheduleConverter;

    private final AtomicBoolean calculationRunning = new AtomicBoolean(false);

    public ScheduleMonth calculateSchedule(
            CalculationProfileForm form
    ) {
        if (!calculationRunning.compareAndSet(false, true)) {
            throw new CalculationAlreadyRunningException("Another schedule calculation is already running.");
        }

        try {
            CalculationInput input = calculationInputLoader.load(form);

            ScheduleCandidate bestCandidate = parallelService.calculateBestSchedule(input);

            return calculatedScheduleConverter.toLegacyScheduleMonth(bestCandidate, form);

        } finally {
            calculationRunning.set(false);
        }
    }

    public boolean isCalculationRunning() {
        return calculationRunning.get();
    }
}