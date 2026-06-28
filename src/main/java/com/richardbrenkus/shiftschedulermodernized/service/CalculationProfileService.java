package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.ApplicationConstants;
import com.richardbrenkus.shiftschedulermodernized.config.SelectionLists;
import com.richardbrenkus.shiftschedulermodernized.config.ShiftTypeProperties;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.MonthOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@Service
@RequiredArgsConstructor
public class CalculationProfileService {

    private final ShiftTypeProperties shiftTypeProperties;

    public List<Integer> getGapBetweenShiftsOptions() {
        return SelectionLists.GENERIC_ONE_TO_TEN_LIST;
    }

    public List<Integer> getShiftCountCapOptions() {
        return SelectionLists.GENERIC_ONE_TO_TEN_LIST;
    }

    public List<Integer> getAvailableShiftTypes() {
        return IntStream.rangeClosed(1, shiftTypeProperties.count())
                .boxed()
                .toList();
    }

    public List<MonthOption> getAvailableCalculationMonths() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");

        return LongStream.range(2, 14)
                .mapToObj(months -> YearMonth.now(ApplicationConstants.ZONE_ID).plusMonths(months))
                .map(month -> new MonthOption(
                        month,
                        month.format(formatter)
                ))
                .toList();
    }

    public List<Integer> resolveShiftCalculationOrder(CalculationProfileForm form) {

        List<Integer> prioritized = form.getForceFillShiftTypes() == null
                ? List.of()
                : form.getForceFillShiftTypes();

        List<Integer> remaining = getAvailableShiftTypes().stream()
                .filter(shiftType -> !prioritized.contains(shiftType))
                .toList();

        List<Integer> result = new ArrayList<>();
        result.addAll(prioritized);
        result.addAll(remaining);

        return result;
    }
}
