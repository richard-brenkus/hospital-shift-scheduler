package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.dto.view.MonthOption;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredScheduleDayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoredScheduleServiceTest {

    @Mock
    private StoredScheduleDayRepository storedScheduleDayRepository;

    @Mock
    private ShiftTypeService shiftTypeService;

    @InjectMocks
    private StoredScheduleService service;

    @Test
    void shouldReturnFalse_whenExistsByMonthIsCalledWithNull() {
        assertThat(service.existsByMonth(null)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenNoStoredDaysFoundForMonth() {
        when(storedScheduleDayRepository.findByMonthYearIdOrderByDayIntegerAsc("08/2026"))
                .thenReturn(List.of());

        assertThat(service.existsByMonth(YearMonth.of(2026, 8))).isFalse();
    }

    @Test
    void shouldThrowIllegalArgumentException_whenSavingNullScheduleMonth() {
        assertThatThrownBy(() -> service.saveScheduleWithStats(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schedule");
    }

    @Test
    void shouldThrowIllegalArgumentException_whenLoadingSavedScheduleViewForNullMonth() {
        assertThatThrownBy(() -> service.loadSavedScheduleView(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnFifteenMonthOptionsStartingTwoMonthsInFuture() {
        List<MonthOption> months = service.getSelectableMonthOptions();

        assertThat(months).hasSize(15);
        YearMonth now = YearMonth.now();
        assertThat(months.get(0).value()).isEqualTo(now.plusMonths(2));
        assertThat(months.get(1).value()).isEqualTo(now.plusMonths(1));
        assertThat(months.get(2).value()).isEqualTo(now);
        assertThat(months.get(3).value()).isEqualTo(now.minusMonths(1));
        assertThat(months.get(14).value()).isEqualTo(now.minusMonths(12));
    }

    @Test
    void shouldFormatMonthOptionLabelsAsMonthSlashYear() {
        List<MonthOption> months = service.getSelectableMonthOptions();
        YearMonth first = months.getFirst().value();

        String expectedLabel = String.format("%02d/%d", first.getMonthValue(), first.getYear());
        assertThat(months.getFirst().label()).isEqualTo(expectedLabel);
    }
}
