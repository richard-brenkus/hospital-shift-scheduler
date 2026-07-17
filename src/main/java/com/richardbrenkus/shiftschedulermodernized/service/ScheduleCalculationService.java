package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculatedScheduleConverter;
import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationInputLoader;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ParallelScheduleCalculationService;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculationInput;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ScheduleCandidate;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleCalculationService {
    private final CalculationInputLoader calculationInputLoader;
    private final ParallelScheduleCalculationService parallelService;
    private final CalculatedScheduleConverter calculatedScheduleConverter;

    public ScheduleMonth calculateSchedule(CalculationProfileForm form) {
        CalculationInput input = calculationInputLoader.load(form);
        ScheduleCandidate bestCandidate = parallelService.calculateBestSchedule(input);
        return calculatedScheduleConverter.toLegacyScheduleMonth(bestCandidate, form);
    }
}
