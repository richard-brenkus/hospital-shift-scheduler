package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalculationCounters;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleValidationResult;
import com.richardbrenkus.shiftschedulermodernized.config.ShiftTypeProperties;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ScheduleEditForm;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ScheduleMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleValidationServiceTest {

    private static final YearMonth AUGUST_2026 = YearMonth.of(2026, 8);

    @Mock
    private ScheduleMapper scheduleMapper;

    @Mock
    private ScheduleRuleService scheduleRuleService;

    @Mock
    private UserStatisticService userStatisticService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private ScheduleValidationService service;

    @BeforeEach
    void setUp() {
        service = new ScheduleValidationService(
                new ShiftTypeProperties(6),
                scheduleMapper,
                scheduleRuleService,
                userStatisticService,
                userRepository,
                userService
        );
    }

    @Test
    void shouldInitializeValidationAndUserStats_whenGivenConvertedWinningSchedule() {
        stubEmptyStatistics();

        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        month.setCalculationProfile(calculationProfile(10, 0));

        ScheduleValidationResult result = service.initializeValidationAndUserStats(month);

        assertThat(result.getScheduleMonth()).isSameAs(month);
        assertThat(result.getScheduleScore()).isEqualTo("0/186");
        assertThat(result.isErrorsExist()).isFalse();
        assertThat(result.isAllUsersExist()).isTrue();
    }

    @Test
    void shouldCountAssignmentsByUserIdBeforeGeneratingStatistics() {
        stubEmptyStatistics();

        User user = TestFixtures.user(1L, "freddie");
        TestFixtures.attachRequest(
                user,
                List.of(),
                TestFixtures.preference(1, 1, 5, 5, true, List.of())
        );

        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        month.setCalculationProfile(calculationProfile(10, 0));
        TestFixtures.assign(month, AUGUST_2026.atDay(3), 1, user);

        service.initializeValidationAndUserStats(month);

        ArgumentCaptor<CalculationCounters> countersCaptor =
                ArgumentCaptor.forClass(CalculationCounters.class);

        verify(userStatisticService).returnQuickUserStats(
                any(ScheduleMonth.class),
                anyInt(),
                countersCaptor.capture()
        );

        CalculationCounters counters = countersCaptor.getValue();
        assertThat(counters.getWeekdayCount(1L, 1)).isEqualTo(1);
        assertThat(counters.getTotalCount(1L)).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyResult_whenValidatingEmptyEditForm() {
        ScheduleEditForm form = ScheduleEditForm.builder().build();

        when(scheduleMapper.toScheduleMonth(
                any(ScheduleEditForm.class),
                any(CalculationProfileForm.class)
        )).thenReturn(null);

        ScheduleValidationResult result = service.validateSchedule(form);

        assertThat(result.isErrorsExist()).isFalse();
        assertThat(result.isAllUsersExist()).isTrue();
        assertThat(result.getScheduleMonth()).isNull();
    }

    @Test
    void shouldThrowIllegalArgumentException_whenScheduleHasNoCalculationProfile() {
        ScheduleEditForm form = ScheduleEditForm.builder()
                .month(AUGUST_2026)
                .build();

        ScheduleMonth mapped = ScheduleMonth.builder()
                .month(AUGUST_2026)
                .build();

        when(scheduleMapper.toScheduleMonth(
                any(ScheduleEditForm.class),
                any(CalculationProfileForm.class)
        )).thenReturn(mapped);

        assertThatThrownBy(() -> service.validateSchedule(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("calculation profile");
    }

    @Test
    void shouldMarkUserNoRequestError_whenAssignedUserHasNoShiftRequestAndOverrideDisabled() {
        stubEmptyStatistics();

        User user = TestFixtures.user(1L, "freddie");
        user.setName("Freddie");

        ScheduleMonth month = preparedMonth(10, 0);
        TestFixtures.assign(month, AUGUST_2026.atDay(5), 1, user);

        stubMappedSchedule(month);
        stubPreviousMonthDays();

        ScheduleValidationResult result = service.validateSchedule(editForm());

        assertThat(result.isErrorsExist()).isTrue();
        assertThat(result.isUserNoRequest()).isTrue();
        assertThat(result.getRedFieldsByShiftType().get(1)).contains(5);
    }

    @Test
    void shouldSkipUserNoRequestMarking_whenOverrideHasShiftRequestEnabled() {
        stubEmptyStatistics();

        User user = TestFixtures.user(1L, "freddie");

        ScheduleMonth month = preparedMonth(10, 0);
        month.setOverrideHasShiftRequest(true);
        TestFixtures.assign(month, AUGUST_2026.atDay(5), 1, user);

        stubMappedSchedule(month);
        stubPreviousMonthDays();

        ScheduleValidationResult result = service.validateSchedule(editForm());

        assertThat(result.isUserNoRequest()).isFalse();
        assertThat(result.isErrorsExist()).isFalse();
    }

    @Test
    void shouldMarkShiftCapError_whenTotalShiftLimitExceededAndOverrideDisabled() {
        stubEmptyStatistics();

        User user = userWithRequest();
        ScheduleMonth month = preparedMonth(1, 0);
        TestFixtures.assign(month, AUGUST_2026.atDay(5), 1, user);

        stubMappedSchedule(month);
        stubPreviousMonthDays();
        allowAllLegacyRulesExceptTotalLimit();

        ScheduleValidationResult result = service.validateSchedule(editForm());

        assertThat(result.isUserShiftCap()).isTrue();
        assertThat(result.isErrorsExist()).isTrue();
        assertThat(result.getRedFieldsByShiftType().get(1)).contains(5);
    }

    @Test
    void shouldNotMarkShiftCapError_whenOverrideShiftCountCapEnabled() {
        stubEmptyStatistics();

        User user = userWithRequest();
        ScheduleMonth month = preparedMonth(1, 0);
        month.setOverrideShiftCountCap(true);

        LocalDate assignedDate = AUGUST_2026.atDay(5);
        TestFixtures.assign(month, assignedDate, 1, user);

        stubMappedSchedule(month);
        stubPreviousMonthDays();

        when(scheduleRuleService.isValidWithinTotalShiftLimit(
                anyInt(),
                any(User.class),
                any(CalculationCounters.class)
        )).thenReturn(false);

        when(scheduleRuleService.isValidWithinRequestedWeekdayLimit(
                any(User.class),
                anyInt(),
                any(CalculationCounters.class)
        )).thenReturn(true);

        when(scheduleRuleService.respectsMinimalGap(
                any(LocalDate.class),
                anyInt(),
                any(User.class),
                any(ScheduleMonth.class),
                anyInt()
        )).thenReturn(true);

        when(scheduleRuleService.isNotRejectedByUser(
                any(LocalDate.class),
                any()
        )).thenReturn(true);

        when(scheduleRuleService.respectsPreviousMonthGap(
                any(Map.class),
                anyInt(),
                any(LocalDate.class),
                any(User.class)
        )).thenReturn(true);

        ScheduleValidationResult result =
                service.validateSchedule(editForm());

        assertThat(result.isUserShiftCap()).isFalse();
        assertThat(result.isErrorsExist()).isFalse();
    }

    @Test
    void shouldMarkCrossCheckError_whenMinimalGapFailsAndOverrideDisabled() {
        stubEmptyStatistics();

        User user = userWithRequest();
        ScheduleMonth month = preparedMonth(10, 2);
        TestFixtures.assign(month, AUGUST_2026.atDay(5), 1, user);

        stubMappedSchedule(month);
        stubPreviousMonthDays();

        ScheduleValidationResult result = service.validateSchedule(editForm());

        assertThat(result.isUserCrossCheck()).isTrue();
        assertThat(result.isErrorsExist()).isTrue();
        assertThat(result.getRedFieldsByShiftType().get(1)).contains(5);
    }

    @Test
    void shouldMarkDatesNoError_whenUserRejectedAssignedDate() {
        stubEmptyStatistics();

        LocalDate assignedDate = AUGUST_2026.atDay(5);
        User user = TestFixtures.user(1L, "freddie");

        TestFixtures.attachRequest(
                user,
                List.of(assignedDate),
                TestFixtures.preference(1, 1, 5, 0, true, List.of())
        );

        ScheduleMonth month = preparedMonth(10, 0);
        TestFixtures.assign(month, assignedDate, 1, user);

        stubMappedSchedule(month);
        stubPreviousMonthDays();
        //allowAllLegacyRules();

        when(scheduleRuleService.isNotRejectedByUser(
                assignedDate,
                user.getShiftRequest().getDatesNo()
        )).thenReturn(false);

        ScheduleValidationResult result = service.validateSchedule(editForm());

        assertThat(result.isUserDatesNo()).isTrue();
        assertThat(result.isErrorsExist()).isTrue();
    }

    @Test
    void shouldMarkPreviousMonthError_whenPreviousMonthGapFails() {
        stubEmptyStatistics();

        User user = userWithRequest();
        ScheduleMonth month = preparedMonth(10, 5);
        TestFixtures.assign(month, AUGUST_2026.atDay(1), 1, user);

        stubMappedSchedule(month);
        stubPreviousMonthDays();
        //allowAllLegacyRules();

       when(scheduleRuleService.respectsPreviousMonthGap(
                any(Map.class),
                anyInt(),
                any(LocalDate.class),
                any(User.class)
        )).thenReturn(false);

        ScheduleValidationResult result = service.validateSchedule(editForm());

        assertThat(result.isPreviousMonthCheckFailed()).isTrue();
        assertThat(result.isErrorsExist()).isTrue();
        assertThat(result.getRedFieldsByShiftType().get(1)).contains(1);
    }

    private void allowAllLegacyRules() {
        when(scheduleRuleService.isValidWithinTotalShiftLimit(
                anyInt(), any(User.class), any(CalculationCounters.class)
        )).thenReturn(true);
        when(scheduleRuleService.isValidWithinRequestedWeekdayLimit(
                any(User.class), anyInt(), any(CalculationCounters.class)
        )).thenReturn(true);
        when(scheduleRuleService.isValidWithinRequestedWeekendLimit(
                any(User.class), anyInt(), any(CalculationCounters.class)
        )).thenReturn(true);
        when(scheduleRuleService.respectsMinimalGap(
                any(LocalDate.class),
                anyInt(),
                any(User.class),
                any(ScheduleMonth.class),
                anyInt()
        )).thenReturn(true);
        when(scheduleRuleService.isNotRejectedByUser(
                any(LocalDate.class), any()
        )).thenReturn(true);
        when(scheduleRuleService.respectsPreviousMonthGap(
                any(Map.class),
                anyInt(),
                any(LocalDate.class),
                any(User.class)
        )).thenReturn(true);
    }

    private void allowAllLegacyRulesExceptTotalLimit() {
        //allowAllLegacyRules();
        when(scheduleRuleService.isValidWithinTotalShiftLimit(
                anyInt(), any(User.class), any(CalculationCounters.class)
        )).thenReturn(false);
    }

    private void stubMappedSchedule(ScheduleMonth month) {
        when(scheduleMapper.toScheduleMonth(
                any(ScheduleEditForm.class),
                any(CalculationProfileForm.class)
        )).thenReturn(month);
    }

    private void stubPreviousMonthDays() {
        when(scheduleRuleService.loadPreviousStoredScheduleDays(
                any(LocalDate.class), anyInt()
        )).thenReturn(Map.of());
    }

    private ScheduleMonth preparedMonth(int shiftCountCap, int minimalGap) {
        ScheduleMonth month = TestFixtures.emptyScheduleMonth(AUGUST_2026);
        month.setCalculationProfile(calculationProfile(shiftCountCap, minimalGap));
        return month;
    }

    private CalculationProfileForm calculationProfile(
            int shiftCountCap,
            int minimalGap
    ) {
        return CalculationProfileForm.builder()
                .calculationMonth(AUGUST_2026)
                .shiftCountCap(shiftCountCap)
                .gapBetweenShifts(minimalGap)
                .forceFillShiftTypes(List.of())
                .build();
    }

    private ScheduleEditForm editForm() {
        return ScheduleEditForm.builder()
                .month(AUGUST_2026)
                .build();
    }

    private User userWithRequest() {
        User user = TestFixtures.user(1L, "freddie");
        user.setName("Freddie");
        TestFixtures.attachRequest(
                user,
                List.of(),
                TestFixtures.preference(1, 1, 5, 0, true, List.of())
        );
        return user;
    }

    private void stubEmptyStatistics() {
        when(userStatisticService.returnQuickUserStats(
                any(ScheduleMonth.class),
                anyInt(),
                any(CalculationCounters.class)
        )).thenReturn(Map.of());

        when(userStatisticService.returnNoShiftAssignedUserStatMap(
                any(ScheduleMonth.class),
                any(CalculationCounters.class)
        )).thenReturn(Map.of());

        when(userStatisticService.returnFullUserStats(
                any(ScheduleMonth.class),
                any(CalculationCounters.class)
        )).thenReturn(Map.of());

        when(userStatisticService.returnScheduleScoreAsString(
                any(ScheduleMonth.class),
                anyInt()
        )).thenReturn("0/186");
    }
}