package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.algorithm.CalculationCounters;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.ScheduleDay;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.ScheduleMonth;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.ShiftAssignment;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.CalculatedScheduleDay;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.CalculatedScheduleMonth;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.CalculatedShiftAssignment;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.ShiftPreferenceCalculationData;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.UserCalculationData;
import com.richardbrenkus.hospitalshiftscheduler.entity.StoredScheduleDay;
import com.richardbrenkus.hospitalshiftscheduler.entity.StoredUserSnapshot;
import com.richardbrenkus.hospitalshiftscheduler.repository.StoredScheduleDayRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        assertThat(service.isNotRejectedByUser(LocalDate.of(2026, 8, 5), Set.of(LocalDate.of(2026, 8, 6)))).isTrue();
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

        assertThat(service.isWithinRequestedWeekdayLimit(user, 1, new CalculationCounters())).isFalse();
    }

    @Test
    void shouldReturnFalseForDtoWeekdayLimit_whenRequestedCountIsZero() {
        UserCalculationData user = calculationUser(1L, Map.of(1, preferenceData(1, 0, 2)), Set.of(), true);

        assertThat(service.isWithinRequestedWeekdayLimit(user, 1, new CalculationCounters())).isFalse();
    }

    @Test
    void shouldReturnTrueForDtoWeekdayLimit_whenCounterIsBelowRequested() {
        UserCalculationData user = calculationUser(1L, Map.of(1, preferenceData(1, 2, 0)), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(1L, 1);

        assertThat(service.isWithinRequestedWeekdayLimit(user, 1, counters)).isTrue();
    }

    @Test
    void shouldReturnFalseForDtoWeekdayLimit_whenCounterEqualsRequested() {
        UserCalculationData user = calculationUser(1L, Map.of(1, preferenceData(1, 1, 0)), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(1L, 1);

        assertThat(service.isWithinRequestedWeekdayLimit(user, 1, counters)).isFalse();
    }

    @Test
    void shouldReturnFalseForDtoWeekendLimit_whenPreferenceIsMissing() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);

        assertThat(service.isWithinRequestedWeekendLimit(user, 1, new CalculationCounters())).isFalse();
    }

    @Test
    void shouldReturnFalseForDtoWeekendLimit_whenRequestedCountIsZero() {
        UserCalculationData user = calculationUser(1L, Map.of(1, preferenceData(1, 2, 0)), Set.of(), true);

        assertThat(service.isWithinRequestedWeekendLimit(user, 1, new CalculationCounters())).isFalse();
    }

    @Test
    void shouldReturnTrueForDtoWeekendLimit_whenCounterIsBelowRequested() {
        UserCalculationData user = calculationUser(1L, Map.of(1, preferenceData(1, 0, 2)), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekend(1L, 1);

        assertThat(service.isWithinRequestedWeekendLimit(user, 1, counters)).isTrue();
    }

    @Test
    void shouldReturnFalseForDtoWeekendLimit_whenCounterEqualsRequested() {
        UserCalculationData user = calculationUser(1L, Map.of(1, preferenceData(1, 0, 1)), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekend(1L, 1);

        assertThat(service.isWithinRequestedWeekendLimit(user, 1, counters)).isFalse();
    }

    @Test
    void shouldReturnTrueForDtoMinimalGap_whenArgumentsAreInvalidOrGapIsZero() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculatedScheduleMonth month = calculatedMonth(List.of());

        assertThat(service.respectsMinimalGap(null, 2, user, month, 1)).isTrue();
        assertThat(service.respectsMinimalGap(LocalDate.of(2026, 8, 10), 2, null, month, 1)).isTrue();
        assertThat(service.respectsMinimalGap(LocalDate.of(2026, 8, 10), 2, user, (CalculatedScheduleMonth) null, 1)).isTrue();
        assertThat(service.respectsMinimalGap(LocalDate.of(2026, 8, 10), 0, user, month, 1)).isTrue();
    }

    @Test
    void shouldReturnFalseForDtoMinimalGap_whenSameUserHasNearbyAssignment() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculatedScheduleMonth month = calculatedMonth(List.of(calculatedDay(LocalDate.of(2026, 8, 9), List.of(new CalculatedShiftAssignment(2, 1L)))));

        assertThat(service.respectsMinimalGap(LocalDate.of(2026, 8, 10), 2, user, month, 1)).isFalse();
    }

    @Test
    void shouldIgnoreSameSlotForDtoMinimalGap() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculatedScheduleMonth month = calculatedMonth(List.of(calculatedDay(LocalDate.of(2026, 8, 10), List.of(new CalculatedShiftAssignment(1, 1L)))));

        assertThat(service.respectsMinimalGap(LocalDate.of(2026, 8, 10), 2, user, month, 1)).isTrue();
    }

    @Test
    void shouldIgnoreDtoAssignmentsOutsideGapAndAssignmentsOfOtherUsers() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculatedScheduleMonth month = calculatedMonth(List.of(calculatedDay(LocalDate.of(2026, 8, 1), List.of(new CalculatedShiftAssignment(1, 1L))), calculatedDay(LocalDate.of(2026, 8, 9), List.of(new CalculatedShiftAssignment(2, 99L)))));

        assertThat(service.respectsMinimalGap(LocalDate.of(2026, 8, 10), 2, user, month, 1)).isTrue();
    }

    @Test
    void shouldReturnTrueForDtoPreviousMonthGap_whenArgumentsAreInvalid() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);

        assertThat(service.respectsPreviousMonthGap(0, LocalDate.of(2026, 8, 1), user, AUGUST_2026)).isTrue();
        assertThat(service.respectsPreviousMonthGap(2, null, user, AUGUST_2026)).isTrue();
        assertThat(service.respectsPreviousMonthGap(2, LocalDate.of(2026, 8, 1), null, AUGUST_2026)).isTrue();
        assertThat(service.respectsPreviousMonthGap(2, LocalDate.of(2026, 8, 1), user, null)).isTrue();
    }

    @Test
    void shouldReturnTrueForDtoPreviousMonthGap_whenCandidateIsOutsideBoundaryWindow() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(LocalDate.of(2026, 7, 31)), true);

        assertThat(service.respectsPreviousMonthGap(2, LocalDate.of(2026, 8, 3), user, AUGUST_2026)).isTrue();
    }

    @Test
    void shouldReturnFalseForDtoPreviousMonthGap_whenAssignmentIsInsideGap() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(LocalDate.of(2026, 7, 31)), true);

        assertThat(service.respectsPreviousMonthGap(2, LocalDate.of(2026, 8, 1), user, AUGUST_2026)).isFalse();
    }

    @Test
    void shouldReturnTrueForDtoPreviousMonthGap_whenDateIsTooOld() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(LocalDate.of(2026, 7, 20)), true);

        assertThat(service.respectsPreviousMonthGap(2, LocalDate.of(2026, 8, 1), user, AUGUST_2026)).isTrue();
    }

    @Test
    void respectsMinimalGap_shouldReturnTrue_whenRequiredArgumentIsMissing() {
        LocalDate date = LocalDate.of(2026, 8, 10);
        UserCalculationData validUser = calculationUser(1L, Map.of(), Set.of(), true);
        ScheduleMonth scheduleMonth = mock(ScheduleMonth.class);

        UserCalculationData userWithoutId = mock(UserCalculationData.class);
        when(userWithoutId.userId()).thenReturn(null);

        assertThat(service.respectsMinimalGap(null, 2, validUser, scheduleMonth, 1)).isTrue();
        assertThat(service.respectsMinimalGap(date, 2, null, scheduleMonth, 1)).isTrue();
        assertThat(service.respectsMinimalGap(date, 2, userWithoutId, scheduleMonth, 1)).isTrue();
        assertThat(service.respectsMinimalGap(date, 2, validUser, scheduleMonth, 1)).isTrue();
    }

    @Test
    void respectsMinimalGap_shouldSkipInvalidDaysAndAssignments() {
        LocalDate candidateDate = LocalDate.of(2026, 8, 10);
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);

        ScheduleDay dayWithoutDate = mock(ScheduleDay.class);
        when(dayWithoutDate.getDate()).thenReturn(null);

        ScheduleDay dayWithoutAssignments = mock(ScheduleDay.class);
        when(dayWithoutAssignments.getDate()).thenReturn(candidateDate);
        when(dayWithoutAssignments.getAssignments()).thenReturn(null);

        ShiftAssignment assignmentWithoutUser = mock(ShiftAssignment.class);
        when(assignmentWithoutUser.getUserCalculationData()).thenReturn(null);

        ScheduleDay dayWithInvalidAssignments = mock(ScheduleDay.class);
        when(dayWithInvalidAssignments.getDate()).thenReturn(candidateDate.minusDays(1));

        List<ShiftAssignment> invalidAssignments = new ArrayList<>();
        invalidAssignments.add(null);
        invalidAssignments.add(assignmentWithoutUser);
        when(dayWithInvalidAssignments.getAssignments()).thenReturn(invalidAssignments);

        List<ScheduleDay> days = new ArrayList<>();
        days.add(null);
        days.add(dayWithoutDate);
        days.add(dayWithoutAssignments);
        days.add(dayWithInvalidAssignments);

        ScheduleMonth scheduleMonth = mock(ScheduleMonth.class);
        when(scheduleMonth.getDays()).thenReturn(days);

        assertThat(service.respectsMinimalGap(candidateDate, 2, user, scheduleMonth, 1)).isTrue();
    }

    @Test
    void respectsMinimalGap_shouldIgnoreAssignmentsOutsideGapWindow() {
        LocalDate candidateDate = LocalDate.of(2026, 8, 10);
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);

        ShiftAssignment sameUserBeforeWindow = mock(ShiftAssignment.class);
        //when(sameUserBeforeWindow.getUserCalculationData()).thenReturn(user);

        ShiftAssignment sameUserAfterWindow = mock(ShiftAssignment.class);
        //when(sameUserAfterWindow.getUserCalculationData()).thenReturn(user);

        ScheduleDay dayBeforeWindow = mock(ScheduleDay.class);
        when(dayBeforeWindow.getDate()).thenReturn(candidateDate.minusDays(3));
        when(dayBeforeWindow.getAssignments()).thenReturn(List.of(sameUserBeforeWindow));

        ScheduleDay dayAfterWindow = mock(ScheduleDay.class);
        when(dayAfterWindow.getDate()).thenReturn(candidateDate.plusDays(3));
        when(dayAfterWindow.getAssignments()).thenReturn(List.of(sameUserAfterWindow));

        ScheduleMonth scheduleMonth = mock(ScheduleMonth.class);
        when(scheduleMonth.getDays()).thenReturn(List.of(dayBeforeWindow, dayAfterWindow));

        assertThat(service.respectsMinimalGap(candidateDate, 2, user, scheduleMonth, 1)).isTrue();
    }

    @Test
    void respectsMinimalGap_shouldIgnoreSameSlotAndAssignmentOfDifferentUser() {
        LocalDate candidateDate = LocalDate.of(2026, 8, 10);
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        UserCalculationData differentUser = calculationUser(2L, Map.of(), Set.of(), true);

        ShiftAssignment sameSlotAssignment = mock(ShiftAssignment.class);
        when(sameSlotAssignment.getShiftType()).thenReturn(1);
        when(sameSlotAssignment.getUserCalculationData()).thenReturn(user);

        ShiftAssignment differentUserAssignment = mock(ShiftAssignment.class);

        ScheduleDay sameDay = mock(ScheduleDay.class);
        when(sameDay.getDate()).thenReturn(candidateDate);
        when(sameDay.getAssignments()).thenReturn(List.of(sameSlotAssignment));

        ScheduleDay nearbyDay = mock(ScheduleDay.class);
        when(nearbyDay.getDate()).thenReturn(candidateDate.plusDays(1));
        when(nearbyDay.getAssignments()).thenReturn(List.of(differentUserAssignment));

        ScheduleMonth scheduleMonth = mock(ScheduleMonth.class);
        when(scheduleMonth.getDays()).thenReturn(List.of(sameDay, nearbyDay));

        assertThat(service.respectsMinimalGap(candidateDate, 2, user, scheduleMonth, 1)).isTrue();
    }

    @Test
    void respectsMinimalGap_shouldReturnFalse_whenSameUserHasAssignmentInsideGap() {
        LocalDate candidateDate = LocalDate.of(2026, 8, 10);
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);

        ShiftAssignment nearbyAssignment = mock(ShiftAssignment.class);
        when(nearbyAssignment.getUserCalculationData()).thenReturn(user);

        ScheduleDay nearbyDay = mock(ScheduleDay.class);
        when(nearbyDay.getDate()).thenReturn(candidateDate.minusDays(1));
        when(nearbyDay.getAssignments()).thenReturn(List.of(nearbyAssignment));

        ScheduleMonth scheduleMonth = mock(ScheduleMonth.class);
        when(scheduleMonth.getDays()).thenReturn(List.of(nearbyDay));

        assertThat(service.respectsMinimalGap(candidateDate, 2, user, scheduleMonth, 1)).isFalse();
    }

    @Test
    void respectsPreviousMonthGap_shouldReturnTrue_whenRequiredArgumentIsMissing() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        UserCalculationData validUser = calculationUser(1L, Map.of(), Set.of(), true);

        UserCalculationData userWithoutUsername = mock(UserCalculationData.class);
        when(userWithoutUsername.username()).thenReturn(null);

        assertThat(service.respectsPreviousMonthGap(null, 2, date, validUser)).isTrue();
        assertThat(service.respectsPreviousMonthGap(Map.of(), null, date, validUser)).isTrue();
        assertThat(service.respectsPreviousMonthGap(Map.of(), 2, null, validUser)).isTrue();
        assertThat(service.respectsPreviousMonthGap(Map.of(), 2, date, null)).isTrue();
        assertThat(service.respectsPreviousMonthGap(Map.of(), 2, date, userWithoutUsername)).isTrue();
    }

    @Test
    void respectsPreviousMonthGap_shouldReturnTrue_whenDateIsOutsideBoundaryWindow() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);

        assertThat(service.respectsPreviousMonthGap(Map.of(), 2, LocalDate.of(2026, 8, 3), user)).isTrue();
    }

    @Test
    void respectsPreviousMonthGap_shouldSkipMissingDaysNullSnapshotsAndOtherUsers() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);

        StoredUserSnapshot otherUserSnapshot = mock(StoredUserSnapshot.class);
        when(otherUserSnapshot.getUsername()).thenReturn("different-user");

        StoredScheduleDay previousMonthDay = mock(StoredScheduleDay.class);

        Map<Integer, StoredUserSnapshot> assignments = new java.util.HashMap<>();
        assignments.put(1, null);
        assignments.put(2, otherUserSnapshot);

        when(previousMonthDay.getAssignmentsByShiftType()).thenReturn(assignments);

        Map<Integer, StoredScheduleDay> previousDays = new java.util.HashMap<>();
        previousDays.put(-1, previousMonthDay);

        assertThat(service.respectsPreviousMonthGap(previousDays, 2, LocalDate.of(2026, 8, 1), user)).isTrue();
    }

    @Test
    void respectsPreviousMonthGap_shouldReturnFalse_whenUserWorkedInsidePreviousMonthGap() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);

        StoredUserSnapshot matchingSnapshot = mock(StoredUserSnapshot.class);
        when(matchingSnapshot.getUsername()).thenReturn(user.username());

        StoredScheduleDay previousMonthDay = mock(StoredScheduleDay.class);
        when(previousMonthDay.getAssignmentsByShiftType()).thenReturn(Map.of(1, matchingSnapshot));

        assertThat(service.respectsPreviousMonthGap(Map.of(-1, previousMonthDay), 2, LocalDate.of(2026, 8, 1), user)).isFalse();
    }

    @Test
    void isValidWithinTotalShiftLimit_shouldReturnFalse_whenUserIsNullOrHasNoRequest() {
        UserCalculationData userWithoutRequest = calculationUser(1L, Map.of(), Set.of(), false);
        CalculationCounters counters = new CalculationCounters();

        assertThat(service.isValidWithinTotalShiftLimit(2, null, counters)).isFalse();

        assertThat(service.isValidWithinTotalShiftLimit(2, userWithoutRequest, counters)).isFalse();
    }

    @Test
    void isValidWithinTotalShiftLimit_shouldReturnTrue_whenCapIsNull() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();

        counters.incrementWeekday(user.userId(), 1);
        counters.incrementWeekend(user.userId(), 1);

        assertThat(service.isValidWithinTotalShiftLimit(null, user, counters)).isTrue();
    }

    @Test
    void isValidWithinTotalShiftLimit_shouldAcceptEqualCountAndRejectCountAboveCap() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();

        counters.incrementWeekday(user.userId(), 1);
        counters.incrementWeekend(user.userId(), 2);

        assertThat(service.isValidWithinTotalShiftLimit(2, user, counters)).isTrue();

        counters.incrementWeekday(user.userId(), 2);

        assertThat(service.isValidWithinTotalShiftLimit(2, user, counters)).isFalse();
    }

    @Test
    void isValidWithinRequestedWeekendLimit_shouldReturnFalse_whenUserIsNullOrHasNoRequest() {
        UserCalculationData userWithoutRequest = calculationUser(1L, Map.of(), Set.of(), false);
        CalculationCounters counters = new CalculationCounters();

        assertThat(service.isValidWithinRequestedWeekendLimit(null, 1, counters)).isFalse();

        assertThat(service.isValidWithinRequestedWeekendLimit(userWithoutRequest, 1, counters)).isFalse();
    }

    @Test
    void isValidWithinRequestedWeekendLimit_shouldUseZero_whenPreferenceIsMissing() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();

        assertThat(service.isValidWithinRequestedWeekendLimit(user, 1, counters)).isTrue();

        counters.incrementWeekend(user.userId(), 1);

        assertThat(service.isValidWithinRequestedWeekendLimit(user, 1, counters)).isFalse();
    }

    @Test
    void isValidWithinRequestedWeekendLimit_shouldAcceptEqualCountAndRejectCountAboveRequest() {
        UserCalculationData user = calculationUser(1L, Map.of(1, preferenceData(1, 0, 1)), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();

        counters.incrementWeekend(user.userId(), 1);

        assertThat(service.isValidWithinRequestedWeekendLimit(user, 1, counters)).isTrue();

        counters.incrementWeekend(user.userId(), 1);

        assertThat(service.isValidWithinRequestedWeekendLimit(user, 1, counters)).isFalse();
    }

    @Test
    void isValidWithinRequestedWeekdayLimit_shouldReturnFalse_whenUserIsNullOrHasNoRequest() {
        UserCalculationData userWithoutRequest = calculationUser(1L, Map.of(), Set.of(), false);
        CalculationCounters counters = new CalculationCounters();

        assertThat(service.isValidWithinRequestedWeekdayLimit(null, 1, counters)).isFalse();

        assertThat(service.isValidWithinRequestedWeekdayLimit(userWithoutRequest, 1, counters)).isFalse();
    }

    @Test
    void isValidWithinRequestedWeekdayLimit_shouldUseZero_whenPreferenceIsMissing() {
        UserCalculationData user = calculationUser(1L, Map.of(), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();

        assertThat(service.isValidWithinRequestedWeekdayLimit(user, 1, counters)).isTrue();

        counters.incrementWeekday(user.userId(), 1);

        assertThat(service.isValidWithinRequestedWeekdayLimit(user, 1, counters)).isFalse();
    }

    @Test
    void isValidWithinRequestedWeekdayLimit_shouldAcceptEqualCountAndRejectCountAboveRequest() {
        UserCalculationData user = calculationUser(1L, Map.of(1, preferenceData(1, 1, 0)), Set.of(), true);
        CalculationCounters counters = new CalculationCounters();

        counters.incrementWeekday(user.userId(), 1);

        assertThat(service.isValidWithinRequestedWeekdayLimit(user, 1, counters)).isTrue();

        counters.incrementWeekday(user.userId(), 1);

        assertThat(service.isValidWithinRequestedWeekdayLimit(user, 1, counters)).isFalse();
    }

    private UserCalculationData calculationUser(Long userId, Map<Integer, ShiftPreferenceCalculationData> preferences, Set<LocalDate> previousMonthAssignedDates, boolean hasShiftRequest) {
        return new UserCalculationData(userId, "User " + userId, "user-" + userId, null, Set.of(1, 2), Set.of(), preferences, previousMonthAssignedDates, hasShiftRequest);
    }

    private ShiftPreferenceCalculationData preferenceData(int shiftType, int weekdayCount, int weekendCount) {
        return new ShiftPreferenceCalculationData(shiftType, 1, weekdayCount, weekendCount, false, true, Set.of());
    }

    private CalculatedScheduleMonth calculatedMonth(List<CalculatedScheduleDay> days) {
        return CalculatedScheduleMonth.builder().month(AUGUST_2026).hitCounter(0).days(new ArrayList<>(days)).build();
    }

    private CalculatedScheduleDay calculatedDay(LocalDate date, List<CalculatedShiftAssignment> assignments) {
        return CalculatedScheduleDay.builder().date(date).weekendOrHoliday(false).assignments(new ArrayList<>(assignments)).build();
    }
}
