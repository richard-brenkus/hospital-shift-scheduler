package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
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

    public ScheduleMonth calculateSchedule(CalculationProfileForm form) {
        if (!calculationRunning.compareAndSet(false, true)) {
            /*
             * ACTIVITY LOG REVIEW:
             *
             * No calculation actually started, so this rejected request should
             * normally not be recorded as SCHEDULE_CALCULATION_FAILED.
             *
             * Log it only if rejected concurrent attempts are important audit
             * information in your application.
             */
            throw new CalculationAlreadyRunningException(
                    "Another schedule calculation is already running."
            );
        }

        String calculationMonth = resolveCalculationMonth(form);

        try {
            /*
             * Keep this call inside the try block. If publishing unexpectedly
             * throws, the finally block must still reset calculationRunning.
             */
            activityPublisher.publishSuccess(
                    ActivityType.SCHEDULE_CALCULATION_STARTED,
                    "ScheduleCalculation",
                    calculationMonth,
                    "Schedule calculation started for " + calculationMonth
            );

            CalculationInput input = calculationInputLoader.load(form);

            ScheduleCandidate bestCandidate =
                    parallelService.calculateBestSchedule(input);

            ScheduleMonth result =
                    calculatedScheduleConverter.toLegacyScheduleMonth(
                            bestCandidate,
                            form
                    );

            activityPublisher.publishSuccess(
                    ActivityType.SCHEDULE_CALCULATION_COMPLETED,
                    "ScheduleCalculation",
                    calculationMonth,
                    "Schedule calculation completed for "
                            + calculationMonth
                            + "; hit counter: "
                            + bestCandidate.hitCounter()
                            + "; worker index: "
                            + bestCandidate.workerIndex()
                            + "; attempt index: "
                            + bestCandidate.attemptIndex()
            );

            return result;

        } catch (RuntimeException exception) {
            /*
             * ACTIVITY LOG REVIEW:
             *
             * Do not call publishSuccess(...) with
             * SCHEDULE_CALCULATION_FAILED.
             *
             * Add a publishFailure(...) method to ActivityPublisher and enable
             * a call here, for example:
             *
             * activityPublisher.publishFailure(
             *         ActivityType.SCHEDULE_CALCULATION_FAILED,
             *         "ScheduleCalculation",
             *         calculationMonth,
             *         "Schedule calculation failed for " + calculationMonth,
             *         "Schedule calculation failed."
             * );
             *
             * Keep the stored error message generic. Do not persist stack
             * traces, SQL errors, passwords, tokens or form contents.
             */
            throw exception;

        } finally {
            calculationRunning.set(false);
        }
    }

    public boolean isCalculationRunning() {
        /*
         * ACTIVITY LOG REVIEW:
         *
         * Read-only status check. Do not log.
         */
        return calculationRunning.get();
    }

    private String resolveCalculationMonth(CalculationProfileForm form) {
        /*
         * ACTIVITY LOG REVIEW:
         *
         * Pure helper method. Do not log.
         */
        if (form == null) {
            return "UNKNOWN";
        }

        YearMonth month = form.getCalculationMonth();
        return month != null ? month.toString() : "UNKNOWN";
    }
}