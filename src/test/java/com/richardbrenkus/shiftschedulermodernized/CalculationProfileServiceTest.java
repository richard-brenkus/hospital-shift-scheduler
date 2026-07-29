package com.richardbrenkus.shiftschedulermodernized;

import com.richardbrenkus.shiftschedulermodernized.config.ShiftTypeProperties;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ApplicationConstants;
import com.richardbrenkus.shiftschedulermodernized.dto.view.MonthOption;
import com.richardbrenkus.shiftschedulermodernized.service.CalculationProfileService;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


class CalculationProfileServiceTest {

    private final CalculationProfileService calculationProfileService = new CalculationProfileService(new ShiftTypeProperties(6));

    @Test
    void shouldReturnNumbersFromOneToTen() {
        assertThat(calculationProfileService.getGenericOneToTenList()).hasSize(10).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    void shouldReturnShiftTypesOneToSix_whenConfiguredCountIsSix() {
        assertThat(calculationProfileService.getAvailableShiftTypes()).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void shouldReturnEmptyShiftTypeList_whenConfiguredCountIsZero() {
        CalculationProfileService service = new CalculationProfileService(new ShiftTypeProperties(0));

        assertThat(service.getAvailableShiftTypes()).isEmpty();
    }

    @Test
    void shouldReturnTwelveMonthsStartingTwoMonthsFromNow_whenListingAvailableCalculationMonths() {
        YearMonth now = YearMonth.now(ApplicationConstants.ZONE_ID);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");

        List<MonthOption> months = calculationProfileService.getAvailableCalculationMonths();

        assertThat(months).hasSize(12);
        assertThat(months.getFirst().value()).isEqualTo(now.plusMonths(2));
        assertThat(months.getFirst().label()).isEqualTo(now.plusMonths(2).format(formatter));
        assertThat(months.get(11).value()).isEqualTo(now.plusMonths(13));
        assertThat(months.get(11).label()).isEqualTo(now.plusMonths(13).format(formatter));
    }
}
