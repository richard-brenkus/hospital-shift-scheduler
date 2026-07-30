package com.richardbrenkus.hospitalshiftscheduler.support;

import com.richardbrenkus.hospitalshiftscheduler.algorithm.ScheduleDay;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.ScheduleMonth;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.ShiftAssignment;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.UserCalculationData;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.Role;
import com.richardbrenkus.hospitalshiftscheduler.entity.ShiftPreference;
import com.richardbrenkus.hospitalshiftscheduler.entity.ShiftRequest;
import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import com.richardbrenkus.hospitalshiftscheduler.mapper.UserCalculationDataMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static User user(long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setName(username);
        user.setEmail(username + "@example.test");
        user.setPassword("hashed-password");
        user.setEnabled(true);
        user.setRole(Role.USER);
        user.setAllowedShiftTypes(new HashSet<>());
        return user;
    }

    public static User admin(long id, String username) {
        User user = user(id, username);
        user.setRole(Role.ADMIN);
        return user;
    }

    public static User userWithAllowedShiftTypes(long id, String username, Integer... shiftTypes) {
        User user = user(id, username);
        user.setAllowedShiftTypes(new HashSet<>(Arrays.asList(shiftTypes)));
        return user;
    }

    public static ShiftPreference preference(int shiftType, int priority, int weekdayCount, int weekendCount, boolean anyDateSelected, List<LocalDate> datesYes) {
        return ShiftPreference.builder()
                .shiftType(shiftType)
                .priority(priority)
                .weekdayCount(weekdayCount)
                .weekendCount(weekendCount)
                .anyDateSelected(anyDateSelected)
                .datesYes(new ArrayList<>(datesYes))
                .noShiftRequested(false)
                .build();
    }

    public static void attachRequest(User user, List<LocalDate> datesNo, ShiftPreference... preferences) {
        ShiftRequest request = new ShiftRequest();
        request.setDatesNo(new ArrayList<>(datesNo));

        List<ShiftPreference> prefList = new ArrayList<>();
        for (ShiftPreference preference : preferences) {
            preference.setShiftRequest(request);
            prefList.add(preference);
        }
        request.setPreferences(prefList);

        user.setShiftRequest(request);
    }

    public static ScheduleMonth emptyScheduleMonth(YearMonth month) {
        List<ScheduleDay> days = IntStream.rangeClosed(1, month.lengthOfMonth()).mapToObj(dayOfMonth -> ScheduleDay.builder().date(month.atDay(dayOfMonth)).weekendOrHoliday(isWeekendDate(month.atDay(dayOfMonth))).assignments(new ArrayList<>()).build()).toList();

        return ScheduleMonth.builder().month(month).days(new ArrayList<>(days)).hitCounter(0).build();
    }

    public static void assign(ScheduleMonth scheduleMonth, LocalDate date, int shiftType, User user) {
        ScheduleDay day = scheduleMonth.getDays().stream().filter(candidate -> candidate.getDate().equals(date)).findFirst().orElseThrow(() -> new IllegalStateException("No day for date " + date));

        day.getAssignments().add(ShiftAssignment.builder().shiftType(shiftType).userCalculationData(toUserCalculationData(user)).build());
    }

    /**
     * Adapter from a JPA {@link User} to the calculation-domain
     * {@link UserCalculationData} snapshot used by the schedule algorithm.
     * <p>
     * Delegates to the production {@link UserCalculationDataMapper} so that the
     * shift preferences attached via {@link #attachRequest(User, java.util.List, ShiftPreference...)}
     * survive into the algorithm-facing snapshot (required by any test that
     * later asks the algorithm to reason about those preferences).
     */
    public static UserCalculationData toUserCalculationData(User user) {
        return USER_CALCULATION_DATA_MAPPER.toCalculationData(user, null);
    }

    private static final UserCalculationDataMapper USER_CALCULATION_DATA_MAPPER = new UserCalculationDataMapper();

    public static Set<Integer> shiftTypeSet(Integer... shiftTypes) {
        return new HashSet<>(Arrays.asList(shiftTypes));
    }

    private static boolean isWeekendDate(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6;
    }
}
