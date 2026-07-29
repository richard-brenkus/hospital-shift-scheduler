package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.algorithm.record.*;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.entity.*;
import com.richardbrenkus.shiftschedulermodernized.mapper.UserCalculationDataMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.service.ScheduleRuleService;
import com.richardbrenkus.shiftschedulermodernized.service.ShiftTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/*
 * NOTE: {@link CalculationInputLoader} now delegates row-level eligibility
 * filtering to {@link UserRepository#findAllByEnabledTrueAndShiftRequestIsNotNullOrderByNameAsc()},
 * which returns a {@link Set}. The old {@code findAll()}-plus-in-memory-filter
 * assumption has been replaced.
 */
@ExtendWith(MockitoExtension.class)
class CalculationInputLoaderTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShiftTypeService shiftTypeService;

    @Mock
    private ScheduleRuleService scheduleRuleService;

    @Mock
    private UserCalculationDataMapper userCalculationDataMapper;

    @InjectMocks
    private CalculationInputLoader loader;

    @Test
    void shouldLoadEligibleUsersAndPutForceFillTypesFirst() {
        User included = mock(User.class);
        when(included.getId()).thenReturn(1L);

        UserCalculationData mapped = calculationUser(1L, Set.of());

        when(userRepository.findAllByEnabledTrueAndShiftRequestIsNotNullOrderByNameAsc()).thenReturn(new LinkedHashSet<>(List.of(included)));
        when(shiftTypeService.getShiftTypes()).thenReturn(List.of(1, 2, 3, 4));
        when(scheduleRuleService.loadPreviousStoredScheduleDays(AUGUST_2026.atDay(1), 5)).thenReturn(Map.of());
        when(userCalculationDataMapper.toCalculationData(included, Set.of())).thenReturn(mapped);

        CalculationInput result = loader.load(CalculationProfileForm.builder().calculationMonth(AUGUST_2026).shiftCountCap(10).gapBetweenShifts(5).sortByDatesAmount(true).forceFillShiftTypes(List.of(3, 1)).build());

        assertThat(result.users()).containsExactly(mapped);
        assertThat(result.shiftTypes()).containsExactly(1, 2, 3, 4);
        assertThat(result.calculationOrder()).containsExactly(3, 1, 2, 4);
        assertThat(result.priorities()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(result.profile().shiftCountCap()).isEqualTo(10);
        assertThat(result.profile().gapBetweenShifts()).isEqualTo(5);
        assertThat(result.profile().sortByDatesAmount()).isTrue();

        verify(userCalculationDataMapper).toCalculationData(included, Set.of());
    }

    @Test
    void shouldMapPreviousStoredAssignmentsToUserDates() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        StoredScheduleDay finalDay = StoredScheduleDay.builder().assignmentsByShiftType(Map.of(1, snapshot(1L))).build();
        StoredScheduleDay previousDay = StoredScheduleDay.builder().assignmentsByShiftType(Map.of(2, snapshot(1L))).build();

        Set<LocalDate> expectedDates = Set.of(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 30));

        when(userRepository.findAllByEnabledTrueAndShiftRequestIsNotNullOrderByNameAsc()).thenReturn(new LinkedHashSet<>(List.of(user)));
        when(shiftTypeService.getShiftTypes()).thenReturn(List.of(1, 2));
        when(scheduleRuleService.loadPreviousStoredScheduleDays(AUGUST_2026.atDay(1), 2)).thenReturn(Map.of(0, finalDay, -1, previousDay));
        when(userCalculationDataMapper.toCalculationData(user, expectedDates)).thenReturn(calculationUser(1L, expectedDates));

        CalculationInput result = loader.load(CalculationProfileForm.builder().calculationMonth(AUGUST_2026).gapBetweenShifts(2).forceFillShiftTypes(List.of()).build());

        assertThat(result.users()).hasSize(1);
        assertThat(result.users().getFirst().previousMonthAssignedDates()).containsExactlyInAnyOrderElementsOf(expectedDates);
    }

    @Test
    void shouldIgnoreNullStoredDaysSnapshotsAndSnapshotIds() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        Map<Integer, StoredUserSnapshot> assignments = new HashMap<>();
        assignments.put(1, null);
        assignments.put(2, StoredUserSnapshot.builder().userId(null).build());

        StoredScheduleDay storedDay = StoredScheduleDay.builder().assignmentsByShiftType(assignments).build();

        when(userRepository.findAllByEnabledTrueAndShiftRequestIsNotNullOrderByNameAsc()).thenReturn(new LinkedHashSet<>(List.of(user)));
        when(shiftTypeService.getShiftTypes()).thenReturn(List.of(1));
        when(scheduleRuleService.loadPreviousStoredScheduleDays(AUGUST_2026.atDay(1), 1)).thenReturn(new HashMap<>() {{
            put(0, storedDay);
            put(-1, null);
        }});
        when(userCalculationDataMapper.toCalculationData(user, Set.of())).thenReturn(calculationUser(1L, Set.of()));

        CalculationInput result = loader.load(CalculationProfileForm.builder().calculationMonth(AUGUST_2026).gapBetweenShifts(1).forceFillShiftTypes(List.of()).build());

        assertThat(result.users().getFirst().previousMonthAssignedDates()).isEmpty();
    }

    @Test
    void shouldCalculateGoodFridayEasterMondayAndChristmas() {
        YearMonth april2026 = YearMonth.of(2026, 4);

        when(userRepository.findAllByEnabledTrueAndShiftRequestIsNotNullOrderByNameAsc()).thenReturn(new LinkedHashSet<>());
        when(shiftTypeService.getShiftTypes()).thenReturn(List.of(1));
        when(scheduleRuleService.loadPreviousStoredScheduleDays(april2026.atDay(1), 0)).thenReturn(Map.of());

        CalculationInput result = loader.load(CalculationProfileForm.builder().calculationMonth(april2026).gapBetweenShifts(0).forceFillShiftTypes(null).build());

        assertThat(result.calculationOrder()).containsExactly(1);
        assertThat(result.profile().forceFillShiftTypes()).isEmpty();
        assertThat(result.holidays()).contains(LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 6), LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 26));
    }

    private StoredUserSnapshot snapshot(Long id) {
        return StoredUserSnapshot.builder().userId(id).username("user-" + id).name("User " + id).build();
    }

    private UserCalculationData calculationUser(Long id, Set<LocalDate> previousDates) {
        return new UserCalculationData(id, "User " + id, "user-" + id, null, Set.of(1, 2), Set.of(), Map.of(), previousDates, true);
    }
}
