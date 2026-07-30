package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.config.ShiftTypeProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShiftTypeServiceTest {

    @Test
    void shouldReturnOneToSixList_whenCountIsSix() {
        ShiftTypeService service = new ShiftTypeService(new ShiftTypeProperties(6));

        assertThat(service.getShiftTypes()).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void shouldReturnEmptyList_whenCountIsZero() {
        ShiftTypeService service = new ShiftTypeService(new ShiftTypeProperties(0));

        assertThat(service.getShiftTypes()).isEmpty();
    }

    @Test
    void shouldReturnSingletonList_whenCountIsOne() {
        ShiftTypeService service = new ShiftTypeService(new ShiftTypeProperties(1));

        assertThat(service.getShiftTypes()).containsExactly(1);
    }
}
