package com.richardbrenkus.shiftschedulermodernized;

import com.richardbrenkus.shiftschedulermodernized.config.ShiftTypeProperties;
import com.richardbrenkus.shiftschedulermodernized.service.CalculationProfileService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


class CalculationProfileServiceTest {

    private final CalculationProfileService calculationProfileService =
            new CalculationProfileService(new ShiftTypeProperties(6));

    @Test
    void shouldReturnNumbersFromOneToTen() {
        assertThat(calculationProfileService.getGenericOneToTenList())
                .hasSize(10)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }
}
