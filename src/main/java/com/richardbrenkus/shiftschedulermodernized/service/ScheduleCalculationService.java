package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.activity.RequestMetadataProvider;
import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculatedScheduleConverter;
import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationInputLoader;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ParallelScheduleCalculationService;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculationInput;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ScheduleCandidate;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.exception.CalculationAlreadyRunningException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class ScheduleCalculationService {

    private final CalculationInputLoader calculationInputLoader;
    private final ParallelScheduleCalculationService parallelService;
    private final CalculatedScheduleConverter calculatedScheduleConverter;
    private final ActivityPublisher activityPublisher;

    private final AtomicBoolean calculationRunning = new AtomicBoolean(false);
    private final RequestMetadataProvider requestMetadataProvider;

    public ScheduleMonth calculateSchedule(CalculationProfileForm form) {
        if (!calculationRunning.compareAndSet(false, true)) {
            throw new CalculationAlreadyRunningException("Another schedule calculation is already running.");
        }

        String calculationMonth = resolveCalculationMonth(form);

        try {
            activityPublisher.publishSuccess(ActivityType.SCHEDULE_CALCULATION_STARTED, "ScheduleCalculation", calculationMonth, "Schedule calculation started for " + calculationMonth);

            CalculationInput input = calculationInputLoader.load(form);

            ScheduleCandidate bestCandidate = parallelService.calculateBestSchedule(input);

            ScheduleMonth result = calculatedScheduleConverter.toLegacyScheduleMonth(bestCandidate, form);

            activityPublisher.publishSuccess(ActivityType.SCHEDULE_CALCULATION_COMPLETED, "ScheduleCalculation", calculationMonth, "Schedule calculation completed for " + calculationMonth + "; hit counter: " + bestCandidate.hitCounter() + "; worker index: " + bestCandidate.workerIndex() + "; attempt index: " + bestCandidate.attemptIndex());

            return result;

        } catch (RuntimeException exception) {
            activityPublisher.publishFailure(ActivityType.SCHEDULE_CALCULATION_FAILED, "ScheduleCalculation", calculationMonth, "Schedule calculation failed for " + calculationMonth, "Schedule calculation failed.", requestMetadataProvider.current());
            throw exception;
        } finally {
            calculationRunning.set(false);
        }
    }

    public boolean isCalculationRunning() {
        return calculationRunning.get();
    }

    private String resolveCalculationMonth(CalculationProfileForm form) {

        if (form == null) {
            return "UNKNOWN";
        }

        YearMonth month = form.getCalculationMonth();

        return month != null ? month.toString() : "UNKNOWN";
    }
}