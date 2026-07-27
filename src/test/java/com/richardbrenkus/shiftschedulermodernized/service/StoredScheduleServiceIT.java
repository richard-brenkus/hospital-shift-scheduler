package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.container.AbstractMySqlContainerTest;
import com.richardbrenkus.shiftschedulermodernized.dto.view.SavedScheduleView;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredScheduleDayRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@Sql(scripts = "/sql/users-basic.sql", executionPhase = BEFORE_TEST_METHOD)
class StoredScheduleServiceIT extends AbstractMySqlContainerTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Autowired
    private StoredScheduleService storedScheduleService;

    @Autowired
    private StoredScheduleDayRepository storedScheduleDayRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void shouldPersistScheduleDaysAndAssignmentsMap_whenSavingSchedule() {
        User alice = userRepository.getUserByUsername("alice.doe");

        ScheduleMonth scheduleMonth = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        addAssignment(scheduleMonth, LocalDate.of(2026, 8, 5), 1, alice);
        addAssignment(scheduleMonth, LocalDate.of(2026, 8, 6), 2, alice);

        storedScheduleService.saveScheduleWithStats(scheduleMonth);

        assertThat(storedScheduleDayRepository.findByMonthYearIdOrderByDayIntegerAsc("08/2026"))
                .hasSize(31);
        assertThat(storedScheduleService.existsByMonth(AUGUST_2026)).isTrue();
    }

    @Test
    @Transactional
    void shouldReadBackAssignmentsAsSavedScheduleView_whenLoadingSavedSchedule() {
        User alice = userRepository.getUserByUsername("alice.doe");
        alice.setTitle("MUDr.");

        ScheduleMonth scheduleMonth = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        addAssignment(scheduleMonth, LocalDate.of(2026, 8, 10), 1, alice);
        storedScheduleService.saveScheduleWithStats(scheduleMonth);

        SavedScheduleView view = storedScheduleService.loadSavedScheduleView(AUGUST_2026);

        assertThat(view.getMonth()).isEqualTo(AUGUST_2026);
        assertThat(view.getDays()).hasSize(31);
        assertThat(view.getDays().get(9).getDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(view.getDays().get(9).getAssignments())
                .anyMatch(assignment -> assignment.getShiftType() == 1
                        && "MUDr. Alice Doe".equals(assignment.getDisplayName()));
    }

    @Test
    @Transactional
    void shouldReturnFalseForExistsByMonth_whenNoDataForThatMonth() {
        assertThat(storedScheduleService.existsByMonth(YearMonth.of(2020, 1))).isFalse();
    }

    private void addAssignment(ScheduleMonth scheduleMonth, LocalDate date, int shiftType, User user) {
        ScheduleDay day = scheduleMonth.getDays().stream()
                .filter(candidate -> candidate.getDate().equals(date))
                .findFirst()
                .orElseThrow();
        if (day.getAssignments() == null) {
            day.setAssignments(new ArrayList<>());
        }
        day.getAssignments().add(ShiftAssignment.builder()
                .shiftType(shiftType)
                .userCalculationData(user)
                .build());
    }
}
