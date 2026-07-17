package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ShiftPreferenceCalculationData;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.UserCalculationData;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftPreference;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredScheduleDayRepository;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ScheduleRuleServiceTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private StoredScheduleDayRepository storedScheduleDayRepository;

    @InjectMocks
    private ScheduleRuleService service;

    @Test
    void shouldReturnTrue_whenTotalShiftCapIsNull() {
        User user = TestFixtures.user(1L, "u");
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);

        assertThat(service.isWithinTotalShiftLimit(null, user, counters)).isTrue();
    }

    @Test
    void shouldReturnTrue_whenTotalShiftCountIsZero() {
        User user = TestFixtures.user(1L, "u");
        CalculationCounters counters = new CalculationCounters();

        assertThat(service.isWithinTotalShiftLimit(1, user, counters)).isTrue();
    }

    @Test
    void shouldReturnTrue_whenTotalShiftCountBelowCap() {
        User user = TestFixtures.user(1L, "u");
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);
        counters.incrementWeekday(user.getId(), 1);

        assertThat(service.isWithinTotalShiftLimit(3, user, counters)).isTrue();
    }

    @Test
    void shouldReturnFalse_whenTotalShiftCountEqualsCap() {
        User user = TestFixtures.user(1L, "u");
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);
        counters.incrementWeekend(user.getId(), 2);

        assertThat(service.isWithinTotalShiftLimit(2, user, counters)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenUserHasNoPreferenceForShiftType() {
        User user = TestFixtures.user(1L, "u");
        TestFixtures.attachRequest(user, List.of());

        assertThat(service.isWithinRequestedWeekdayLimit(user, 1, new CalculationCounters())).isFalse();
    }

    @Test
    void shouldReturnFalse_whenRequestedWeekdayCountIsZero() {
        User user = TestFixtures.user(1L, "u");
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 0, 2, false, List.of()));

        assertThat(service.isWithinRequestedWeekdayLimit(user, 1, new CalculationCounters())).isFalse();
    }

    @Test
    void shouldReturnTrue_whenWeekdayCounterBelowRequested() {
        User user = TestFixtures.user(1L, "u");
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 2, 0, false, List.of()));
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);

        assertThat(service.isWithinRequestedWeekdayLimit(user, 1, counters)).isTrue();
    }

    @Test
    void shouldReturnFalse_whenWeekdayCounterEqualsRequested() {
        User user = TestFixtures.user(1L, "u");
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 1, 0, false, List.of()));
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);

        assertThat(service.isWithinRequestedWeekdayLimit(user, 1, counters)).isFalse();
    }

    @Test
    void shouldReturnTrue_whenWeekendCounterBelowRequested() {
        User user = TestFixtures.user(1L, "u");
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 0, 2, false, List.of()));
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekend(user.getId(), 1);

        assertThat(service.isWithinRequestedWeekendLimit(user, 1, counters)).isTrue();
    }

    @Test
    void shouldReturnTrue_whenDateIsNull() {
        User user = TestFixtures.user(1L, "u");
        assertThat(service.respectsMinimalGap(null, 2, user, TestFixtures.emptyScheduleMonth(YearMonth.of(2026, 8)), 1)).isTrue();
    }

    @Test
    void shouldReturnTrue_whenNoOtherAssignmentsWithinGap() {
        User user = TestFixtures.user(1L, "u");
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(YearMonth.of(2026, 8));

        assertThat(service.respectsMinimalGap(LocalDate.of(2026, 8, 10), 2, user, month, 1)).isTrue();
    }

    @Test
    void shouldReturnFalse_whenUserHasNearbyAssignmentInDifferentShiftType() {
        User user = TestFixtures.user(1L, "u");
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(YearMonth.of(2026, 8));
        TestFixtures.assign(month, LocalDate.of(2026, 8, 9), 2, user);

        assertThat(service.respectsMinimalGap(LocalDate.of(2026, 8, 10), 2, user, month, 1)).isFalse();
    }

    @Test
    void shouldIgnoreSameSlotAssignment_whenCheckingMinimalGap() {
        User user = TestFixtures.user(1L, "u");
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(YearMonth.of(2026, 8));
        TestFixtures.assign(month, LocalDate.of(2026, 8, 10), 1, user);

        assertThat(service.respectsMinimalGap(LocalDate.of(2026, 8, 10), 2, user, month, 1)).isTrue();
    }

    @Test
    void shouldReturnTrueForPreviousMonthGap_whenMapIsNull() {
        User user = TestFixtures.user(1L, "u");
        assertThat(service.respectsPreviousMonthGap(null, 2, LocalDate.of(2026, 8, 1), user)).isTrue();
    }

    @Test
    void shouldReturnTrueForPreviousMonthGap_whenDayIsAfterGapWindow() {
        User user = TestFixtures.user(1L, "u");
        assertThat(service.respectsPreviousMonthGap(Map.of(), 2, LocalDate.of(2026, 8, 10), user)).isTrue();
    }

    @Test
    void shouldReturnFalseForPreviousMonthGap_whenUserWorkedNearBoundary() {
        User user = TestFixtures.user(1L, "u");
        Map<Integer, StoredScheduleDay> previous = new HashMap<>();
        StoredScheduleDay boundaryDay = StoredScheduleDay.builder()
                .assignmentsByShiftType(new HashMap<>())
                .build();
        boundaryDay.putAssignment(1, 1L, user.getUsername(), user.getName(), null);
        previous.put(0, boundaryDay);

        assertThat(service.respectsPreviousMonthGap(previous, 2, LocalDate.of(2026, 8, 1), user)).isFalse();
    }

    @Test
    void shouldReturnTrueForPreviousMonthGap_whenAnotherUserWorkedThere() {
        User user = TestFixtures.user(1L, "u");
        Map<Integer, StoredScheduleDay> previous = new HashMap<>();
        StoredScheduleDay boundaryDay = StoredScheduleDay.builder()
                .assignmentsByShiftType(new HashMap<>())
                .build();
        boundaryDay.putAssignment(1, 5L, "other", "Other", null);
        previous.put(0, boundaryDay);

        assertThat(service.respectsPreviousMonthGap(previous, 2, LocalDate.of(2026, 8, 1), user)).isTrue();
    }

    @Test
    void shouldReturnTrue_whenDatesNoIsNull() {
        assertThat(service.isNotRejectedByUser(LocalDate.of(2026, 8, 1), null)).isTrue();
    }

    @Test
    void shouldReturnFalse_whenDateAppearsInDatesNo() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        assertThat(service.isNotRejectedByUser(date, List.of(date))).isFalse();
    }

    @Test
    void shouldReturnTrue_whenDateNotInDatesNo() {
        assertThat(service.isNotRejectedByUser(
                LocalDate.of(2026, 8, 5),
                List.of(LocalDate.of(2026, 8, 6)))
        ).isTrue();
    }

    @Test
    void shouldReturnFalseForValidTotalShiftLimit_whenUserHasNoShiftRequest() {
        User user = TestFixtures.user(1L, "u");
        assertThat(service.isValidWithinTotalShiftLimit(3, user, new CalculationCounters())).isFalse();
    }

    @Test
    void shouldReturnTrueForValidTotalShiftLimit_whenCapIsNull() {
        User user = TestFixtures.user(1L, "u");
        TestFixtures.attachRequest(user, List.of());
        assertThat(service.isValidWithinTotalShiftLimit(null, user, new CalculationCounters())).isTrue();
    }

    @Test
    void shouldReturnTrueForValidTotalShiftLimit_whenBelowOrEqualCap() {
        User user = TestFixtures.user(1L, "u");
        TestFixtures.attachRequest(user, List.of());
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);

        assertThat(service.isValidWithinTotalShiftLimit(1, user, counters)).isTrue();
    }

    @Test
    void shouldReturnFalseForValidRequestedWeekdayLimit_whenPreferenceMissingAndCountIsPositive() {
        User user = TestFixtures.user(1L, "u");
        TestFixtures.attachRequest(user, List.of()); // no preference for shiftType 1
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);

        assertThat(service.isValidWithinRequestedWeekdayLimit(user, 1, counters)).isFalse();
    }

    @Test
    void shouldReturnTrueForValidRequestedWeekdayLimit_whenAssignedEqualsRequested() {
        User user = TestFixtures.user(1L, "u");
        ShiftPreference pref = TestFixtures.preference(1, 1, 2, 0, false, List.of());
        TestFixtures.attachRequest(user, List.of(), pref);
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);
        counters.incrementWeekday(user.getId(), 1);

        assertThat(service.isValidWithinRequestedWeekdayLimit(user, 1, counters)).isTrue();
    }

    @Test
    void shouldReturnFalseForValidRequestedWeekdayLimit_whenAssignedExceedsRequested() {
        User user = TestFixtures.user(1L, "u");
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 1, 0, false, List.of()));
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);
        counters.incrementWeekday(user.getId(), 1);

        assertThat(service.isValidWithinRequestedWeekdayLimit(user, 1, counters)).isFalse();
    }

    @Test
    void shouldReturnTrueForValidRequestedWeekendLimit_whenAssignedEqualsRequested() {
        User user = TestFixtures.user(1L, "u");
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 0, 1, false, List.of()));
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekend(user.getId(), 1);

        assertThat(service.isValidWithinRequestedWeekendLimit(user, 1, counters)).isTrue();
    }


    // ---------------------------------------------------------------------
    // UserCalculationData overloads used by multithreaded calculation
    // ---------------------------------------------------------------------

    @Test
    void shouldReturnTrueForDtoTotalShiftLimit_whenCapIsNull() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of());
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(1L, 1);

        assertThat(service.isWithinTotalShiftLimit(null, user, counters)).isTrue();
    }

    @Test
    void shouldReturnTrueForDtoTotalShiftLimit_whenBelowCap() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of());
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(1L, 1);
        counters.incrementWeekend(1L, 2);

        assertThat(service.isWithinTotalShiftLimit(3, user, counters)).isTrue();
    }

    @Test
    void shouldReturnFalseForDtoTotalShiftLimit_whenCountEqualsCap() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of());
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(1L, 1);
        counters.incrementWeekend(1L, 2);

        assertThat(service.isWithinTotalShiftLimit(2, user, counters)).isFalse();
    }

    @Test
    void shouldReturnFalseForDtoWeekdayLimit_whenPreferenceIsMissing() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of());

        assertThat(service.isWithinRequestedWeekdayLimit(
                user, 1, new CalculationCounters()
        )).isFalse();
    }

    @Test
    void shouldReturnFalseForDtoWeekdayLimit_whenRequestedCountIsZero() {
        UserCalculationData user = calculationUser(
                1L,
                Map.of(1, preferenceData(1, 0, 2)),
                Set.of()
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
                Set.of()
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
                Set.of()
        );
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(1L, 1);

        assertThat(service.isWithinRequestedWeekdayLimit(
                user, 1, counters
        )).isFalse();
    }

    @Test
    void shouldReturnFalseForDtoWeekendLimit_whenPreferenceIsMissing() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of());

        assertThat(service.isWithinRequestedWeekendLimit(
                user, 1, new CalculationCounters()
        )).isFalse();
    }

    @Test
    void shouldReturnFalseForDtoWeekendLimit_whenRequestedCountIsZero() {
        UserCalculationData user = calculationUser(
                1L,
                Map.of(1, preferenceData(1, 2, 0)),
                Set.of()
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
                Set.of()
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
                Set.of()
        );
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekend(1L, 1);

        assertThat(service.isWithinRequestedWeekendLimit(
                user, 1, counters
        )).isFalse();
    }

    @Test
    void shouldReturnTrueForDtoMinimalGap_whenArgumentsAreInvalidOrGapIsZero() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of());
        CalculatedScheduleMonth month = calculatedMonth(List.of());

        assertThat(service.respectsMinimalGap(null, 2, user, month, 1)).isTrue();
        assertThat(service.respectsMinimalGap(
                LocalDate.of(2026, 8, 10), 2, null, month, 1
        )).isTrue();
        assertThat(service.respectsMinimalGap(
                LocalDate.of(2026, 8, 10), 2, user, null, 1
        )).isTrue();
        assertThat(service.respectsMinimalGap(
                LocalDate.of(2026, 8, 10), 0, user, month, 1
        )).isTrue();
    }

    @Test
    void shouldReturnFalseForDtoMinimalGap_whenSameUserHasNearbyAssignment() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of());
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
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of());
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
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of());
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
    void shouldIgnoreNullDtoScheduleElementsWhileCheckingMinimalGap() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of());

        CalculatedScheduleDay nullDate = CalculatedScheduleDay.builder()
                .date(null)
                .assignments(new ArrayList<>())
                .build();

        CalculatedScheduleDay nullAssignments = CalculatedScheduleDay.builder()
                .date(LocalDate.of(2026, 8, 10))
                .assignments(null)
                .build();

        List<CalculatedShiftAssignment> assignments = new ArrayList<>();
        assignments.add(null);

        CalculatedScheduleDay nullEntry = CalculatedScheduleDay.builder()
                .date(LocalDate.of(2026, 8, 10))
                .assignments(assignments)
                .build();

        List<CalculatedScheduleDay> days = new ArrayList<>();
        days.add(null);
        days.add(nullDate);
        days.add(nullAssignments);
        days.add(nullEntry);

        assertThat(service.respectsMinimalGap(
                LocalDate.of(2026, 8, 10),
                2,
                user,
                calculatedMonth(days),
                1
        )).isTrue();
    }

    @Test
    void shouldReturnTrueForDtoPreviousMonthGap_whenArgumentsAreInvalid() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of());

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
                Set.of(LocalDate.of(2026, 7, 31))
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
                Set.of(LocalDate.of(2026, 7, 31))
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
                Set.of(LocalDate.of(2026, 7, 20))
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
            Set<LocalDate> previousMonthAssignedDates
    ) {
        return new UserCalculationData(
                userId,
                "User " + userId,
                "user-" + userId,
                null,
                Set.of(1, 2),
                Set.of(),
                preferences,
                previousMonthAssignedDates
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
