package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.container.AbstractMySqlContainerTest;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredScheduleDay;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@Sql(scripts = "/sql/stored-schedule-days-cross-month.sql", executionPhase = BEFORE_TEST_METHOD)
class ScheduleRuleServiceIT extends AbstractMySqlContainerTest {

    @Autowired
    private ScheduleRuleService scheduleRuleService;

    @Test
    @Transactional
    void shouldLoadPreviousMonthDaysIndexedByBackwardOffset_whenGapCoversMonthBoundary() {
        // First day of August 2026 with gap of 2 -> checks 2026-07-31 (offset 0) and 2026-07-30 (offset -1).
        LocalDate firstOfAugust = LocalDate.of(2026, 8, 1);

        Map<Integer, StoredScheduleDay> previous =
                scheduleRuleService.loadPreviousStoredScheduleDays(firstOfAugust, 2);

        assertThat(previous).containsOnlyKeys(0, -1);
        assertThat(previous.get(0).getDateId()).isEqualTo(20260731L);
        assertThat(previous.get(-1).getDateId()).isEqualTo(20260730L);
        assertThat(previous.get(0).getAssignmentsByShiftType())
                .containsKey(2);
        assertThat(previous.get(-1).getAssignmentsByShiftType())
                .containsKey(1);
    }

    @Test
    void shouldReturnEmptyMap_whenNoPreviousDaysExistForRequestedGap() {
        // First day of January 2020 - no fixtures cover the previous month days.
        Map<Integer, StoredScheduleDay> previous =
                scheduleRuleService.loadPreviousStoredScheduleDays(LocalDate.of(2020, 1, 1), 3);

        assertThat(previous).isEmpty();
    }
}
