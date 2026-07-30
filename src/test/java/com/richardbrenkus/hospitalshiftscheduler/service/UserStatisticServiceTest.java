package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.algorithm.CalculationCounters;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.ScheduleMonth;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.ScheduleValidationResult;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.ScheduleEditForm;
import com.richardbrenkus.hospitalshiftscheduler.dto.view.UserStatViewRecord;
import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import com.richardbrenkus.hospitalshiftscheduler.mapper.ScheduleMapper;
import com.richardbrenkus.hospitalshiftscheduler.mapper.UserCalculationDataMapper;
import com.richardbrenkus.hospitalshiftscheduler.repository.UserRepository;
import com.richardbrenkus.hospitalshiftscheduler.repository.UserStatRepository;
import com.richardbrenkus.hospitalshiftscheduler.support.TestFixtures;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * NOTE: UserStatisticService now uses named repository queries
 * (findByShiftRequestIsNotNullOrderByNameAsc, findDistinctNamesWithoutShiftRequest)
 * and delegates snapshotting to {@link UserCalculationDataMapper}. Tests were
 * updated to stub those queries and use the real mapper (so that preferences
 * attached in the fixture reach the snapshot the service reasons about).
 */
@ExtendWith(MockitoExtension.class)
class UserStatisticServiceTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShiftTypeService shiftTypeService;

    @Mock
    private ScheduleMapper scheduleMapper;

    @Mock
    private UserStatRepository userStatRepository;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    private UserStatisticService service;

    @BeforeEach
    void setUp() {
        // Real mapper — no external dependencies, and its preference-copying
        // behaviour is what makes the stat calculations meaningful in tests.
        service = new UserStatisticService(userRepository, shiftTypeService, scheduleMapper, userStatRepository, new UserCalculationDataMapper());
    }

    @Test
    void shouldStoreFullStatisticsInSession_whenResultAndFormArePresent() {
        Map<Integer, Set<UserStatViewRecord>> fullStats = Map.of(1, Set.of());
        ScheduleValidationResult result = ScheduleValidationResult.builder().fullUserStatsByShiftType(new java.util.HashMap<>(fullStats)).build();
        ScheduleEditForm form = ScheduleEditForm.builder().month(AUGUST_2026).build();

        service.storeFullStatisticsInSession(session, result, form);

        verify(session).setAttribute("fullUserStatsByShiftType", result.getFullUserStatsByShiftType());
        verify(session).setAttribute("fullStatsMonth", AUGUST_2026);
    }

    @Test
    void shouldClearSession_whenValidationResultIsNull() {
        service.storeFullStatisticsInSession(session, null, ScheduleEditForm.builder().build());

        verify(session).removeAttribute("fullUserStatsByShiftType");
        verify(session).removeAttribute("fullStatsMonth");
    }

    @Test
    void shouldDoNothing_whenSessionIsNull() {
        service.storeFullStatisticsInSession(null, null, null);
    }

    @Test
    void shouldRemoveBothSessionAttributes_whenClearingFullStatistics() {
        service.clearFullStatistics(session);

        verify(session).removeAttribute("fullUserStatsByShiftType");
        verify(session).removeAttribute("fullStatsMonth");
    }

    @Test
    void shouldNotThrow_whenClearingWithNullSession() {
        service.clearFullStatistics(null);
    }

    @Test
    void shouldAddEmptyStatsFlagFalse_whenSessionHasNothing() {
        when(session.getAttribute("fullUserStatsByShiftType")).thenReturn(null);
        when(session.getAttribute("fullStatsMonth")).thenReturn(null);

        service.addFullStatisticsToModel(model, session, List.of(1, 2));

        verify(model).addAttribute("fullUserStatsByShiftType", Map.of());
        verify(model).addAttribute("shiftTypes", List.of(1, 2));
        verify(model).addAttribute("statsExist", false);
        verify(model, never()).addAttribute(org.mockito.ArgumentMatchers.eq("month"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldAddStatsAndMonthAttributesToModel_whenSessionContainsStats() {
        Map<Integer, Set<UserStatViewRecord>> stats = Map.of(1, Set.of(UserStatViewRecord.builder().name("Freddie").build()));
        when(session.getAttribute("fullUserStatsByShiftType")).thenReturn(stats);
        when(session.getAttribute("fullStatsMonth")).thenReturn(AUGUST_2026);

        service.addFullStatisticsToModel(model, session, List.of(1));

        verify(model).addAttribute("fullUserStatsByShiftType", stats);
        verify(model).addAttribute("statsExist", true);
        verify(model).addAttribute("month", AUGUST_2026);
        verify(model).addAttribute("year", 2026);
        verify(model).addAttribute("monthInt", 8);
    }

    @Test
    void shouldReturnEmptyMap_whenReturnQuickUserStatsGetsNullScheduleMonth() {
        assertThat(service.returnQuickUserStats(null, 5, new CalculationCounters())).isEmpty();
    }

    @Test
    void shouldReturnQuickUserStats_whenPreferenceAppliesAndUnderCap() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 1);
        TestFixtures.attachRequest(user, List.of(), TestFixtures.preference(1, 1, 3, 1, false, List.of(date)));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        TestFixtures.assign(month, date, 1, user);
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);

        Map<Integer, Set<UserStatViewRecord>> result = service.returnQuickUserStats(month, 10, counters);

        assertThat(result).containsKey(1);
        assertThat(result.get(1)).hasSize(1);
        UserStatViewRecord record = result.get(1).iterator().next();
        assertThat(record.name()).isEqualTo("freddie");
        assertThat(record.remainingWeekdays()).isEqualTo(2);
        assertThat(record.remainingWeekends()).isEqualTo(1);
        assertThat(record.assignedWeekdays()).isEqualTo(1);
    }

    @Test
    void shouldNotAddQuickUserStatEntry_whenTotalCountReachesCap() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 1);
        TestFixtures.attachRequest(user, List.of(), TestFixtures.preference(1, 1, 5, 0, false, List.of(date)));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        TestFixtures.assign(month, date, 1, user);
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);
        counters.incrementWeekday(user.getId(), 1);

        Map<Integer, Set<UserStatViewRecord>> result = service.returnQuickUserStats(month, 2, counters);

        assertThat(result.getOrDefault(1, Set.of())).isEmpty();
    }

    @Test
    void shouldReturnUsersWithRequestNotAssignedForShiftType_whenNoShiftAssigned() {
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 1);
        TestFixtures.attachRequest(user, List.of(), TestFixtures.preference(1, 1, 5, 0, false, List.of(LocalDate.of(2026, 8, 3))));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        when(userRepository.findByShiftRequestIsNotNullOrderByNameAsc()).thenReturn(List.of(user));
        when(shiftTypeService.getShiftTypes()).thenReturn(List.of(1));

        Map<Integer, Set<UserStatViewRecord>> result = service.returnNoShiftAssignedUserStatMap(month, new CalculationCounters());

        assertThat(result.get(1)).hasSize(1);
    }

    @Test
    void shouldNotReportUserAsNoShiftAssigned_whenUserWasAssignedAnywhere() {
        LocalDate date = LocalDate.of(2026, 8, 3);
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 1);
        TestFixtures.attachRequest(user, List.of(), TestFixtures.preference(1, 1, 5, 0, false, List.of(date)));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        TestFixtures.assign(month, date, 1, user);
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);
        when(userRepository.findByShiftRequestIsNotNullOrderByNameAsc()).thenReturn(List.of(user));
        when(shiftTypeService.getShiftTypes()).thenReturn(List.of(1));

        Map<Integer, Set<UserStatViewRecord>> result = service.returnNoShiftAssignedUserStatMap(month, counters);

        assertThat(result.get(1)).isEmpty();
    }

    @Test
    void shouldReturnSortedUserNamesWithoutRequest() {
        // The service delegates entirely to the repository query, which is
        // expected to sort and de-duplicate. Return the desired list verbatim.
        when(userRepository.findDistinctNamesWithoutShiftRequest()).thenReturn(List.of("Amy", "Zoe"));

        List<String> names = service.returnUsersWithNoRequest();

        assertThat(names).containsExactly("Amy", "Zoe");
    }

    @Test
    void shouldReturnScheduleScoreString_whenGivenMonthAndAssignments() {
        User user = TestFixtures.user(1L, "u");
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        TestFixtures.assign(month, LocalDate.of(2026, 8, 1), 1, user);
        TestFixtures.assign(month, LocalDate.of(2026, 8, 2), 1, user);

        String score = service.returnScheduleScoreAsString(month, 6);

        assertThat(score).isEqualTo("2/" + (31 * 6));
    }

    @Test
    void shouldReturnZeroScore_whenScheduleMonthIsNull() {
        assertThat(service.returnScheduleScoreAsString(null, 6)).isEqualTo("0/0");
    }

    @Test
    void shouldConvertDatesToDayOfMonthSet_ignoringForeignMonthDates() {
        // java.util.Set.of() forbids nulls; use a mutable set so we can pass one
        // and prove the method itself skips it.
        Set<LocalDate> input = new HashSet<>(Arrays.asList(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 9, 1), null));

        Set<Integer> days = service.toCurrentMonthDayOfMonthSet(input, AUGUST_2026);

        assertThat(days).containsExactly(5, 10);
    }

    @Test
    void shouldReturnEmptySet_whenNullDatesGiven() {
        assertThat(service.toCurrentMonthDayOfMonthSet(null, AUGUST_2026)).isEmpty();
    }

    @Test
    void shouldThrowIllegalArgumentException_whenReplacingStatsWithNullYearMonth() {
        assertThatThrownBy(() -> service.replaceStatsForMonth(null, Map.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDeleteAndSkipSaving_whenReplacingStatsWithEmptyMap() {
        service.replaceStatsForMonth(AUGUST_2026, Map.of());

        verify(userStatRepository).deleteByYearMonth(AUGUST_2026);
        verify(userStatRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldReturnFullUserStats_withAssignedDateDaysAndAllShiftsTotal() {
        LocalDate date1 = LocalDate.of(2026, 8, 5);
        LocalDate date2 = LocalDate.of(2026, 8, 6);
        User user = TestFixtures.userWithAllowedShiftTypes(1L, "freddie", 1);
        user.setName("Freddie");
        TestFixtures.attachRequest(user, List.of(), TestFixtures.preference(1, 1, 3, 0, false, List.of(date1, date2)));
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        TestFixtures.assign(month, date1, 1, user);
        TestFixtures.assign(month, date2, 1, user);
        CalculationCounters counters = new CalculationCounters();
        counters.incrementWeekday(user.getId(), 1);
        counters.incrementWeekday(user.getId(), 1);

        Map<Integer, Set<UserStatViewRecord>> result = service.returnFullUserStats(month, counters);

        assertThat(result).containsKey(1);
        UserStatViewRecord record = result.get(1).iterator().next();
        assertThat(record.assignedDateDays()).containsExactly(5, 6);
        assertThat(record.assignedTotalAllShiftTypes()).isEqualTo(2);
    }
}
