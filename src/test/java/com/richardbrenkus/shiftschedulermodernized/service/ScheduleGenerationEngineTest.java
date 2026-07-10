package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.UsersForShiftType;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleGenerationEngineTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private ScheduleRuleService scheduleRuleService;

    @InjectMocks
    private ScheduleGenerationEngine engine;

    @Test
    void shouldAssignUserAndIncrementHitCounter_whenAllRulesPass() {
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 1);
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 5, 0, true, List.of()));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        Map<Integer, UsersForShiftType> usersByShiftType = Map.of(
                1, UsersForShiftType.builder()
                        .specificDateUsers(List.of())
                        .anyDateUsers(List.of(user))
                        .build()
        );
        allowAllRules();

        int hitCounter = engine.assignRegularShifts(
                month,
                List.of(1),
                List.of(1),
                List.of(1),
                usersByShiftType,
                List.of(),
                false,
                10,
                0,
                Map.of(),
                new CalculationCounters()
        );

        assertThat(hitCounter).isEqualTo(1);
        assertThat(hasAssignmentOnDay(month, 1, 1)).isTrue();
    }

    @Test
    void shouldReturnZeroHits_whenMinimalGapCheckFails() {
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 1);
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 5, 0, true, List.of()));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        Map<Integer, UsersForShiftType> usersByShiftType = Map.of(
                1, UsersForShiftType.builder()
                        .specificDateUsers(List.of())
                        .anyDateUsers(List.of(user))
                        .build()
        );
        when(scheduleRuleService.isWithinTotalShiftLimit(anyInt(), any(), any())).thenReturn(true);
        when(scheduleRuleService.isWithinRequestedWeekdayLimit(any(), anyInt(), any())).thenReturn(true);
        when(scheduleRuleService.isWithinRequestedWeekendLimit(any(), anyInt(), any())).thenReturn(true);
        when(scheduleRuleService.respectsMinimalGap(any(), anyInt(), any(), any(), anyInt())).thenReturn(false);
        when(scheduleRuleService.respectsPreviousMonthGap(any(), any(), any(), any())).thenReturn(true);

        int hitCounter = engine.assignRegularShifts(
                month,
                List.of(1, 2, 3),
                List.of(1),
                List.of(1),
                usersByShiftType,
                List.of(),
                false,
                10,
                2,
                Map.of(),
                new CalculationCounters()
        );

        assertThat(hitCounter).isZero();
        assertThat(anyAssignmentInMonth(month)).isFalse();
    }

    @Test
    void shouldSkipUserWithoutMatchingPreferenceInMonth_whenNotAnyDateAndNoDatesYesInMonth() {
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 1);
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 5, 0, false, List.of(LocalDate.of(2026, 12, 1))));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        Map<Integer, UsersForShiftType> usersByShiftType = Map.of(
                1, UsersForShiftType.builder()
                        .specificDateUsers(List.of(user))
                        .anyDateUsers(List.of())
                        .build()
        );
        allowAllRules();

        int hitCounter = engine.assignRegularShifts(
                month,
                List.of(1),
                List.of(1),
                List.of(1),
                usersByShiftType,
                List.of(),
                false,
                10,
                0,
                Map.of(),
                new CalculationCounters()
        );

        assertThat(hitCounter).isZero();
    }

    @Test
    void shouldOnlyProcessForceFillShiftTypes_whenCallingForceFillPath() {
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 2);
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(2, 1, 5, 0, true, List.of()));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        Map<Integer, UsersForShiftType> usersByShiftType = Map.of(
                2, UsersForShiftType.builder()
                        .specificDateUsers(List.of())
                        .anyDateUsers(List.of(user))
                        .build()
        );
        allowAllRules();

        int hitCounter = engine.assignForceFillShifts(
                month,
                List.of(1),
                List.of(1),
                List.of(1, 2),
                usersByShiftType,
                List.of(2),
                false,
                10,
                0,
                Map.of(),
                new CalculationCounters()
        );

        assertThat(hitCounter).isEqualTo(1);
        assertThat(hasAssignmentOnDay(month, 1, 2)).isTrue();
        assertThat(hasAssignmentOnDay(month, 1, 1)).isFalse();
    }

    @Test
    void shouldSkipForceFillShiftTypesInRegularPath() {
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 2);
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(2, 1, 5, 0, true, List.of()));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        Map<Integer, UsersForShiftType> usersByShiftType = Map.of(
                2, UsersForShiftType.builder()
                        .specificDateUsers(List.of())
                        .anyDateUsers(List.of(user))
                        .build()
        );
        allowAllRules();

        int hitCounter = engine.assignRegularShifts(
                month,
                List.of(1),
                List.of(1),
                List.of(2),
                usersByShiftType,
                List.of(2),
                false,
                10,
                0,
                Map.of(),
                new CalculationCounters()
        );

        assertThat(hitCounter).isZero();
    }

    @Test
    void shouldNotAssignUserRejectingTheDate_whenDateIsInDatesNo() {
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 1);
        LocalDate day1 = AUGUST_2026.atDay(1);
        TestFixtures.attachRequest(user, List.of(day1),
                TestFixtures.preference(1, 1, 5, 0, true, List.of()));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        Map<Integer, UsersForShiftType> usersByShiftType = Map.of(
                1, UsersForShiftType.builder()
                        .specificDateUsers(List.of())
                        .anyDateUsers(List.of(user))
                        .build()
        );
        allowAllRules();

        int hitCounter = engine.assignRegularShifts(
                month,
                List.of(1),
                List.of(1),
                List.of(1),
                usersByShiftType,
                List.of(),
                false,
                10,
                0,
                Map.of(),
                new CalculationCounters()
        );

        assertThat(hitCounter).isZero();
    }

    @Test
    void shouldOnlyAssignOnceEvenWhenSameShiftTypeVisitedMultipleTimes() {
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 1);
        TestFixtures.attachRequest(user, List.of(),
                TestFixtures.preference(1, 1, 5, 0, true, List.of()));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        Map<Integer, UsersForShiftType> usersByShiftType = Map.of(
                1, UsersForShiftType.builder()
                        .specificDateUsers(List.of())
                        .anyDateUsers(List.of(user))
                        .build()
        );
        allowAllRules();

        int hitCounter = engine.assignRegularShifts(
                month,
                List.of(1),
                List.of(1),
                List.of(1),
                usersByShiftType,
                List.of(),
                false,
                10,
                0,
                Map.of(),
                new CalculationCounters()
        );

        long assignmentsOnDay1 = month.getDays().stream()
                .filter(day -> day.getDate().equals(AUGUST_2026.atDay(1)))
                .flatMap(day -> day.getAssignments().stream())
                .filter(assignment -> assignment.getShiftType() == 1)
                .count();
        assertThat(assignmentsOnDay1).isEqualTo(1);
        assertThat(hitCounter).isEqualTo(1);
    }

    private void allowAllRules() {
        when(scheduleRuleService.isWithinTotalShiftLimit(anyInt(), any(), any())).thenReturn(true);
        when(scheduleRuleService.isWithinRequestedWeekdayLimit(any(), anyInt(), any())).thenReturn(true);
        when(scheduleRuleService.isWithinRequestedWeekendLimit(any(), anyInt(), any())).thenReturn(true);
        when(scheduleRuleService.respectsMinimalGap(any(), anyInt(), any(), any(), anyInt())).thenReturn(true);
        when(scheduleRuleService.respectsPreviousMonthGap(any(), any(), any(), any())).thenReturn(true);
    }

    private boolean hasAssignmentOnDay(ScheduleMonth month, int dayOfMonth, int shiftType) {
        return month.getDays().stream()
                .filter(day -> day.getDate().equals(AUGUST_2026.atDay(dayOfMonth)))
                .flatMap(day -> day.getAssignments().stream())
                .anyMatch(assignment -> assignment.getShiftType() == shiftType);
    }

    private boolean anyAssignmentInMonth(ScheduleMonth month) {
        return month.getDays().stream()
                .flatMap(day -> day.getAssignments().stream())
                .findAny()
                .isPresent();
    }
}
