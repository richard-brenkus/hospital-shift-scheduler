package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ShiftPreferenceCalculationData;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.UserCalculationData;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredScheduleDayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * NOTE: The original generated tests were structurally obsolete after
 * ScheduleRuleService.isWithinRequestedWeekdayLimit / isWithinRequestedWeekendLimit
 * (and the "isValid*" family) were converted to UserCalculationData-only
 * overloads and after respectsMinimalGap / respectsPreviousMonthGap gained
 * CalculatedScheduleMonth / YearMonth variants. The current tests below
 * exercise the UserCalculationData-based public contract.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleRuleServiceTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private StoredScheduleDayRepository storedScheduleDayRepository;

    @InjectMocks
    private ScheduleRuleService service;

    @Test
    void shouldReturnTrue_whenDatesNoIsNull() {
        assertThat(service.isNotRejectedByUser(LocalDate.of(2026, 8, 1), null)).isTrue();
    }

    @Test
    void shouldReturnFalse_whenDateAppearsInDatesNo() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        assertThat(service.isNotRejectedByUser(date, Set.of(date))).isFalse();
    }

    @Test
    void shouldReturnTrue_whenDateNotInDatesNo() {
        assertThat(service.isNotRejectedByUser(
                LocalDate.of(2026, 8, 5),
                Set.of(LocalDate.of(2026, 8, 6)))
        ).isTrue();
    }

    @Test
    void shouldReturnTrueForDtoTotalShiftLimit_whenCapIsNull() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(1L, 1);

        assertThat(service.isWithinTotalShiftLimit(null, user, counters)).isTrue();
    }

    @Test
    void shouldReturnTrueForDtoTotalShiftLimit_whenBelowCap() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(1L, 1);
        counters.incrementWeekend(1L, 2);

        assertThat(service.isWithinTotalShiftLimit(3, user, counters)).isTrue();
    }

    @Test
    void shouldReturnFalseForDtoTotalShiftLimit_whenCountEqualsCap() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(1L, 1);
        counters.incrementWeekend(1L, 2);

        assertThat(service.isWithinTotalShiftLimit(2, user, counters)).isFalse();
    }

    @Test
    void shouldReturnFalseForDtoWeekdayLimit_whenPreferenceIsMissing() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);

        assertThat(service.isWithinRequestedWeekdayLimit(
                user, 1, new CalculationCounters()
        )).isFalse();
    }

    @Test
    void shouldReturnFalseForDtoWeekdayLimit_whenRequestedCountIsZero() {
        UserCalculationData user = calculationUser(
                1L,
                Map.of(1, preferenceData(1, 0, 2)),
                Set.of(),
                true
        );

        assertThat(service.isWithinRequestedWeekdayLimit(
                user, 1, new CalculationCounters()
        )).isFalse();
    }

    @Test
    void shouldReturnTrueForDtoWeekdayLimit_whenCounterIsBelowRequested() {
        UserCalculationData user = calculationUser(
                1L,
                Map.of(1, preferenceData(1, 2, 0)),
                Set.of(),
                true
        );
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(1L, 1);

        assertThat(service.isWithinRequestedWeekdayLimit(
                user, 1, counters
        )).isTrue();
    }

    @Test
    void shouldReturnFalseForDtoWeekdayLimit_whenCounterEqualsRequested() {
        UserCalculationData user = calculationUser(
                1L,
                Map.of(1, preferenceData(1, 1, 0)),
                Set.of(),
                true
        );
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(1L, 1);

        assertThat(service.isWithinRequestedWeekdayLimit(
                user, 1, counters
        )).isFalse();
    }

    @Test
    void shouldReturnFalseForDtoWeekendLimit_whenPreferenceIsMissing() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);

        assertThat(service.isWithinRequestedWeekendLimit(
                user, 1, new CalculationCounters()
        )).isFalse();
    }

    @Test
    void shouldReturnFalseForDtoWeekendLimit_whenRequestedCountIsZero() {
        UserCalculationData user = calculationUser(
                1L,
                Map.of(1, preferenceData(1, 2, 0)),
                Set.of(),
                true
        );

        assertThat(service.isWithinRequestedWeekendLimit(
                user, 1, new CalculationCounters()
        )).isFalse();
    }

    @Test
    void shouldReturnTrueForDtoWeekendLimit_whenCounterIsBelowRequested() {
        UserCalculationData user = calculationUser(
                1L,
                Map.of(1, preferenceData(1, 0, 2)),
                Set.of(),
                true
        );
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekend(1L, 1);

        assertThat(service.isWithinRequestedWeekendLimit(
                user, 1, counters
        )).isTrue();
    }

    @Test
    void shouldReturnFalseForDtoWeekendLimit_whenCounterEqualsRequested() {
        UserCalculationData user = calculationUser(
                1L,
                Map.of(1, preferenceData(1, 0, 1)),
                Set.of(),
                true
        );
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekend(1L, 1);

        assertThat(service.isWithinRequestedWeekendLimit(
                user, 1, counters
        )).isFalse();
    }

    @Test
    void shouldReturnTrueForDtoMinimalGap_whenArgumentsAreInvalidOrGapIsZero() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculatedScheduleMonth month = calculatedMonth(List.of());

        assertThat(service.respectsMinimalGap(null, 2, user, month, 1)).isTrue();
        assertThat(service.respectsMinimalGap(
                LocalDate.of(2026, 8, 10), 2, null, month, 1
        )).isTrue();
        assertThat(service.respectsMinimalGap(
                LocalDate.of(2026, 8, 10), 2, user, (CalculatedScheduleMonth) null, 1
        )).isTrue();
        assertThat(service.respectsMinimalGap(
                LocalDate.of(2026, 8, 10), 0, user, month, 1
        )).isTrue();
    }

    @Test
    void shouldReturnFalseForDtoMinimalGap_whenSameUserHasNearbyAssignment() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculatedScheduleMonth month = calculatedMonth(List.of(
                calculatedDay(
                        LocalDate.of(2026, 8, 9),
                        List.of(new CalculatedShiftAssignment(2, 1L))
                )
        ));

        assertThat(service.respectsMinimalGap(
                LocalDate.of(2026, 8, 10), 2, user, month, 1
        )).isFalse();
    }

    @Test
    void shouldIgnoreSameSlotForDtoMinimalGap() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculatedScheduleMonth month = calculatedMonth(List.of(
                calculatedDay(
                        LocalDate.of(2026, 8, 10),
                        List.of(new CalculatedShiftAssignment(1, 1L))
                )
        ));

        assertThat(service.respectsMinimalGap(
                LocalDate.of(2026, 8, 10), 2, user, month, 1
        )).isTrue();
    }

    @Test
    void shouldIgnoreDtoAssignmentsOutsideGapAndAssignmentsOfOtherUsers() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculatedScheduleMonth month = calculatedMonth(List.of(
                calculatedDay(
                        LocalDate.of(2026, 8, 1),
                        List.of(new CalculatedShiftAssignment(1, 1L))
                ),
                calculatedDay(
                        LocalDate.of(2026, 8, 9),
                        List.of(new CalculatedShiftAssignment(2, 99L))
                )
        ));

        assertThat(service.respectsMinimalGap(
                LocalDate.of(2026, 8, 10), 2, user, month, 1
        )).isTrue();
    }

    @Test
    void shouldReturnTrueForDtoPreviousMonthGap_whenArgumentsAreInvalid() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);

        assertThat(service.respectsPreviousMonthGap(
                0, LocalDate.of(2026, 8, 1), user, AUGUST_2026
        )).isTrue();
        assertThat(service.respectsPreviousMonthGap(
                2, null, user, AUGUST_2026
        )).isTrue();
        assertThat(service.respectsPreviousMonthGap(
                2, LocalDate.of(2026, 8, 1), null, AUGUST_2026
        )).isTrue();
        assertThat(service.respectsPreviousMonthGap(
                2, LocalDate.of(2026, 8, 1), user, null
        )).isTrue();
    }

    @Test
    void shouldReturnTrueForDtoPreviousMonthGap_whenCandidateIsOutsideBoundaryWindow() {
        UserCalculationData user = calculationUser(
                1L,
                Map.of(),
                Set.of(LocalDate.of(2026, 7, 31)),
                true
        );

        assertThat(service.respectsPreviousMonthGap(
                2,
                LocalDate.of(2026, 8, 3),
                user,
                AUGUST_2026
        )).isTrue();
    }

    @Test
    void shouldReturnFalseForDtoPreviousMonthGap_whenAssignmentIsInsideGap() {
        UserCalculationData user = calculationUser(
                1L,
                Map.of(),
                Set.of(LocalDate.of(2026, 7, 31)),
                true
        );

        assertThat(service.respectsPreviousMonthGap(
                2,
                LocalDate.of(2026, 8, 1),
                user,
                AUGUST_2026
        )).isFalse();
    }

    @Test
    void shouldReturnTrueForDtoPreviousMonthGap_whenDateIsTooOld() {
        UserCalculationData user = calculationUser(
                1L,
                Map.of(),
                Set.of(LocalDate.of(2026, 7, 20)),
                true
        );

        assertThat(service.respectsPreviousMonthGap(
                2,
                LocalDate.of(2026, 8, 1),
                user,
                AUGUST_2026
        )).isTrue();
    }

    private UserCalculationData calculationUser(
            Long userId,
            Map<Integer, ShiftPreferenceCalculationData> preferences,
            Set<LocalDate> previousMonthAssignedDates,
            boolean hasShiftRequest
    ) {
        return new UserCalculationData(
                userId,
                "User " + userId,
                "user-" + userId,
                null,
                Set.of(1, 2),
                Set.of(),
                preferences,
                previousMonthAssignedDates,
                hasShiftRequest
        );
    }

    private ShiftPreferenceCalculationData preferenceData(
            int shiftType,
            int weekdayCount,
            int weekendCount
    ) {
        return new ShiftPreferenceCalculationData(
                shiftType,
                1,
                weekdayCount,
                weekendCount,
                false,
                true,
                Set.of()
        );
    }

    private CalculatedScheduleMonth calculatedMonth(
            List<CalculatedScheduleDay> days
    ) {
        return CalculatedScheduleMonth.builder()
                .month(AUGUST_2026)
                .hitCounter(0)
                .days(new ArrayList<>(days))
                .build();
    }

    private CalculatedScheduleDay calculatedDay(
            LocalDate date,
            List<CalculatedShiftAssignment> assignments
    ) {
        return CalculatedScheduleDay.builder()
                .date(date)
                .weekendOrHoliday(false)
                .assignments(new ArrayList<>(assignments))
                .build();
    }
}
