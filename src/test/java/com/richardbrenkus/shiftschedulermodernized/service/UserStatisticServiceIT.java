package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.container.AbstractMySqlContainerTest;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserStatViewRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.entity.UserStatEntity;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.UserStatRepository;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@Sql(scripts = "/sql/users-basic.sql", executionPhase = BEFORE_TEST_METHOD)
class UserStatisticServiceIT extends AbstractMySqlContainerTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Autowired
    private UserStatisticService userStatisticService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserStatRepository userStatRepository;

    @Test
    @Transactional
    void shouldPersistStatsForMonth_whenReplacingStats() {
        User alice = userRepository.findByUsername("alice.doe").orElseThrow();

        UserStatViewRecord record = UserStatViewRecord.builder()
                .userCalculationData(TestFixtures.toUserCalculationData(alice))
                .name(alice.getName())
                .shiftType(1)
                .requestedWeekdays(3)
                .requestedWeekends(1)
                .calculatedWeekdays(1)
                .calculatedWeekends(0)
                .remainingWeekdays(2)
                .remainingWeekends(1)
                .anyDateSelected(false)
                .requestedDateDays(new TreeSet<>(Set.of(5, 12)))
                .assignedWeekdays(1)
                .assignedWeekends(0)
                .assignedTotal(1)
                .assignedTotalAllShiftTypes(1)
                .assignedDateDays(new TreeSet<>(Set.of(5)))
                .month(AUGUST_2026)
                .build();

        userStatisticService.replaceStatsForMonth(AUGUST_2026, Map.of(1, Set.of(record)));

        List<UserStatEntity> saved =
                userStatRepository.findByYearMonthOrderByShiftTypeAscNameAsc(AUGUST_2026);
        assertThat(saved).hasSize(1);
        UserStatEntity persistedEntity = saved.getFirst();
        assertThat(persistedEntity.getShiftType()).isEqualTo(1);
        assertThat(persistedEntity.getName()).isEqualTo("Alice Doe");
        assertThat(persistedEntity.getRequestedWeekdays()).isEqualTo(3);
        assertThat(persistedEntity.getRequestedDateDays()).containsExactly(5, 12);
        assertThat(persistedEntity.getAssignedDateDays()).containsExactly(5);
    }

    @Test
    @Transactional
    void shouldReplaceExistingStatsForMonth_whenReplacingStats() {
        User alice = userRepository.findByUsername("alice.doe").orElseThrow();
        UserStatViewRecord first = viewRecord(alice, 1, 2);
        userStatisticService.replaceStatsForMonth(AUGUST_2026, Map.of(1, Set.of(first)));
        assertThat(userStatRepository.findByYearMonthOrderByShiftTypeAscNameAsc(AUGUST_2026)).hasSize(1);

        UserStatViewRecord replacement = viewRecord(alice, 2, 5);
        userStatisticService.replaceStatsForMonth(AUGUST_2026, Map.of(2, Set.of(replacement)));

        List<UserStatEntity> reloaded =
                userStatRepository.findByYearMonthOrderByShiftTypeAscNameAsc(AUGUST_2026);
        assertThat(reloaded).hasSize(1);
        assertThat(reloaded.getFirst().getShiftType()).isEqualTo(2);
        assertThat(reloaded.getFirst().getRequestedWeekdays()).isEqualTo(5);
    }

    @Test
    @Transactional
    void shouldDeleteExistingRowsWithoutInsertingNew_whenReplacingWithEmptyMap() {
        User alice = userRepository.findByUsername("alice.doe").orElseThrow();
        userStatisticService.replaceStatsForMonth(AUGUST_2026,
                Map.of(1, Set.of(viewRecord(alice, 1, 1))));
        assertThat(userStatRepository.findByYearMonthOrderByShiftTypeAscNameAsc(AUGUST_2026)).hasSize(1);

        userStatisticService.replaceStatsForMonth(AUGUST_2026, Map.of());

        assertThat(userStatRepository.findByYearMonthOrderByShiftTypeAscNameAsc(AUGUST_2026)).isEmpty();
    }

    @Test
    @Transactional
    void shouldReturnEmptyMap_whenLookingUpViewRecordsForNullOrUnknownMonth() {
        assertThat(userStatisticService.findViewRecordsByYearMonth(null)).isEmpty();
        assertThat(userStatisticService.findViewRecordsByYearMonth(YearMonth.of(1999, 1))).isEmpty();
    }

    @Test
    @Transactional
    void shouldGroupViewRecordsByShiftType_whenLookingUpViewRecords() {
        User alice = userRepository.findByUsername("alice.doe").orElseThrow();
        UserStatViewRecord a = viewRecord(alice, 1, 2);
        UserStatViewRecord b = viewRecord(alice, 1, 3);
        UserStatViewRecord c = viewRecord(alice, 2, 4);
        userStatisticService.replaceStatsForMonth(AUGUST_2026, Map.of(1, Set.of(a, b), 2, Set.of(c)));

        Map<Integer, Set<UserStatViewRecord>> results =
                userStatisticService.findViewRecordsByYearMonth(AUGUST_2026);

        assertThat(results.keySet()).containsExactlyInAnyOrder(1, 2);
        assertThat(results.get(1)).hasSize(2);
        assertThat(results.get(2)).hasSize(1);
    }

    private UserStatViewRecord viewRecord(User user, int shiftType, int requestedWeekdays) {
        return UserStatViewRecord.builder()
                .userCalculationData(TestFixtures.toUserCalculationData(user))
                .name(user.getName())
                .shiftType(shiftType)
                .requestedWeekdays(requestedWeekdays)
                .requestedWeekends(0)
                .calculatedWeekdays(0)
                .calculatedWeekends(0)
                .remainingWeekdays(requestedWeekdays)
                .remainingWeekends(0)
                .anyDateSelected(false)
                .requestedDateDays(new TreeSet<>())
                .assignedWeekdays(0)
                .assignedWeekends(0)
                .assignedTotal(0)
                .assignedTotalAllShiftTypes(0)
                .assignedDateDays(new TreeSet<>())
                .month(AUGUST_2026)
                .build();
    }
}
