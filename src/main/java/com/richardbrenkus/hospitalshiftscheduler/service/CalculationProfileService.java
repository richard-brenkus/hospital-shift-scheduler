package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.ApplicationConstants;
import com.richardbrenkus.hospitalshiftscheduler.config.SelectionLists;
import com.richardbrenkus.hospitalshiftscheduler.config.ShiftTypeProperties;
import com.richardbrenkus.hospitalshiftscheduler.dto.view.MonthOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@Service
@RequiredArgsConstructor
public class CalculationProfileService {

    private final ShiftTypeProperties shiftTypeProperties;

    public List<Integer> getGenericOneToTenList() {
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
                .mapToObj(months -> YearMonth.now(ApplicationConstants.ZONE_ID).plusMonths(months)).map(month -> new MonthOption(month, month.format(formatter)))
                .toList();
    }
}
