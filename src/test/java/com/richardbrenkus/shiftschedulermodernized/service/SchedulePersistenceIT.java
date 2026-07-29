package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleValidationResult;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.UserCalculationData;
import com.richardbrenkus.shiftschedulermodernized.container.AbstractMySqlContainerTest;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserStatViewRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.entity.UserStatEntity;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredScheduleDayRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.UserStatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Verifies that {@link StoredScheduleService#saveScheduleWithStats} treats the
 * schedule-day save and the {@link UserStatisticService#replaceStatsForMonth}
 * call as a single transactional unit:
 *
 * <ul>
 *   <li>a successful call persists both the days and the statistics;</li>
 *   <li>a runtime failure inside statistics persistence rolls back the days;</li>
 *   <li>a runtime failure inside schedule persistence prevents statistics
 *       from being replaced.</li>
 * </ul>
 * <p>
 * The test exercises the public orchestrating method as required — it does not
 * bypass into the two collaborating services directly.
 */
class SchedulePersistenceIT extends AbstractMySqlContainerTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Autowired
    private StoredScheduleService storedScheduleService;

    @Autowired
    private StoredScheduleDayRepository storedScheduleDayRepository;

    @Autowired
    private UserStatRepository userStatRepository;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private UserStatisticService userStatisticService;

    @BeforeEach
    void cleanState() {
        org.springframework.transaction.support.TransactionTemplate transactionTemplate = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            userStatRepository.deleteByYearMonth(AUGUST_2026);
            storedScheduleDayRepository.deleteAll();
        });
    }

    @Test
    void saveScheduleWithStats_shouldPersistDaysAndStatsInOneTransaction() {
        ScheduleMonth month = emptyMonth();
        assign(month, LocalDate.of(2026, 8, 5), 1, 42L);

        ScheduleValidationResult validationResult = validationResultWithStats(42L, 1);

        storedScheduleService.saveScheduleWithStats(month, validationResult);

        List<StoredScheduleDay> storedDays = storedScheduleDayRepository.findByMonthYearIdOrderByDayIntegerAsc("08/2026");
        assertThat(storedDays).hasSize(31);

        List<UserStatEntity> storedStats = userStatRepository.findByYearMonthOrderByShiftTypeAscNameAsc(AUGUST_2026);
        assertThat(storedStats).hasSize(1);
        assertThat(storedStats.getFirst().getUserId()).isEqualTo(42L);
    }

    @Test
    void saveScheduleWithStats_shouldRollBackScheduleSave_whenStatsReplacementThrows() {
        ScheduleMonth month = emptyMonth();
        assign(month, LocalDate.of(2026, 8, 5), 1, 42L);
        ScheduleValidationResult validationResult = validationResultWithStats(42L, 1);

        doThrow(new RuntimeException("simulated stats failure")).when(userStatisticService).replaceStatsForMonth(any(YearMonth.class), any());

        assertThatThrownBy(() -> storedScheduleService.saveScheduleWithStats(month, validationResult)).isInstanceOf(RuntimeException.class).hasMessageContaining("simulated stats failure");

        assertThat(storedScheduleDayRepository.findByMonthYearIdOrderByDayIntegerAsc("08/2026")).as("schedule days must be rolled back when statistics persistence fails").isEmpty();
        assertThat(userStatRepository.findByYearMonthOrderByShiftTypeAscNameAsc(AUGUST_2026)).isEmpty();
    }

    @Test
    void saveScheduleWithStats_shouldNotReplaceStats_whenScheduleSaveFails() {
        // Seed an existing stats row so we can verify it survives.
        UserStatViewRecord seedStat = statViewRecord(42L, 1);
        userStatisticService.replaceStatsForMonth(AUGUST_2026, Map.of(1, Set.of(seedStat)));
        long seededCount = userStatRepository.findByYearMonthOrderByShiftTypeAscNameAsc(AUGUST_2026).size();
        assertThat(seededCount).isEqualTo(1);

        // Fail schedule save by feeding an invalid month.
        ScheduleMonth badMonth = ScheduleMonth.builder().month(AUGUST_2026).days(List.of()).build();
        ScheduleValidationResult validationResult = validationResultWithStats(42L, 1);

        assertThatThrownBy(() -> storedScheduleService.saveScheduleWithStats(badMonth, validationResult)).isInstanceOf(IllegalArgumentException.class);

        List<UserStatEntity> preserved = userStatRepository.findByYearMonthOrderByShiftTypeAscNameAsc(AUGUST_2026);
        assertThat(preserved).as("statistics must not be touched when the schedule cannot be saved").hasSize(1);
        assertThat(preserved.getFirst().getUserId()).isEqualTo(42L);
    }

    @Test
    void saveScheduleWithStats_shouldOverwriteExistingStatsForSameMonth() {
        // Save first version.
        ScheduleMonth month = emptyMonth();
        assign(month, LocalDate.of(2026, 8, 5), 1, 42L);
        storedScheduleService.saveScheduleWithStats(month, validationResultWithStats(42L, 1));

        // Save second version with a different user.
        ScheduleMonth month2 = emptyMonth();
        assign(month2, LocalDate.of(2026, 8, 5), 1, 43L);
        storedScheduleService.saveScheduleWithStats(month2, validationResultWithStats(43L, 1));

        List<UserStatEntity> stats = userStatRepository.findByYearMonthOrderByShiftTypeAscNameAsc(AUGUST_2026);
        assertThat(stats).hasSize(1);
        assertThat(stats.getFirst().getUserId()).isEqualTo(43L);
    }

    private ScheduleMonth emptyMonth() {
        List<ScheduleDay> days = new ArrayList<>();
        for (int day = 1; day <= AUGUST_2026.lengthOfMonth(); day++) {
            days.add(ScheduleDay.builder().date(AUGUST_2026.atDay(day)).weekendOrHoliday(false).assignments(new ArrayList<>()).build());
        }
        return ScheduleMonth.builder().month(AUGUST_2026).days(days).build();
    }

    private void assign(ScheduleMonth month, LocalDate date, int shiftType, long userId) {
        ScheduleDay day = month.getDays().stream().filter(d -> d.getDate().equals(date)).findFirst().orElseThrow();
        day.getAssignments().add(ShiftAssignment.builder().shiftType(shiftType).userCalculationData(minimalCalculationData(userId)).build());
    }

    private static UserCalculationData minimalCalculationData(long userId) {
        return new UserCalculationData(userId, "User " + userId, "user-" + userId, null, Set.of(1), Set.of(), Map.of(), Set.of(), true);
    }

    private static ScheduleValidationResult validationResultWithStats(long userId, int shiftType) {
        Map<Integer, Set<UserStatViewRecord>> stats = Map.of(shiftType, Set.of(statViewRecord(userId, shiftType)));
        ScheduleValidationResult result = ScheduleValidationResult.builder().build();
        result.setFullUserStatsByShiftType(stats);
        return result;
    }

    private static UserStatViewRecord statViewRecord(long userId, int shiftType) {
        return UserStatViewRecord.builder().userCalculationData(minimalCalculationData(userId)).name("User " + userId).shiftType(shiftType).requestedWeekdays(1).requestedWeekends(0).calculatedWeekdays(1).calculatedWeekends(0).remainingWeekdays(0).remainingWeekends(0).anyDateSelected(false).requestedDateDays(new TreeSet<>()).assignedWeekdays(1).assignedWeekends(0).assignedTotal(1).assignedTotalAllShiftTypes(1).assignedDateDays(new TreeSet<>(Set.of(5))).month(AUGUST_2026).build();
    }
}
