package com.richardbrenkus.hospitalshiftscheduler.algorithm;

import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.CalculatedScheduleMonth;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.CalculatedShiftAssignment;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.ScheduleCandidate;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.UserCalculationData;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.CalculationProfileForm;
import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import com.richardbrenkus.hospitalshiftscheduler.mapper.UserCalculationDataMapper;
import com.richardbrenkus.hospitalshiftscheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CalculatedScheduleConverter {

    private final UserRepository userRepository;
    private final UserCalculationDataMapper userCalculationDataMapper;

    @Transactional(readOnly = true)
    public ScheduleMonth toLegacyScheduleMonth(ScheduleCandidate candidate, CalculationProfileForm originalForm) {
        CalculatedScheduleMonth calculated = candidate.scheduleMonth();

        Set<Long> userIds = calculated.getDays().stream()
                .flatMap(day -> day.getAssignments().stream())
                .map(CalculatedShiftAssignment::userId)
                .collect(Collectors.toSet());

        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return ScheduleMonth.builder()
                .month(calculated.getMonth())
                .hitCounter(candidate.hitCounter())
                .days(calculated.getDays().stream()
                        .map(day -> ScheduleDay.builder()
                                .date(day.getDate())
                                .weekendOrHoliday(day.isWeekendOrHoliday())
                                .assignments(day.getAssignments().stream()
                                        .map(assignment -> ShiftAssignment.builder()
                                                .shiftType(assignment.shiftType())
                                                .userCalculationData(requireUser(usersById, assignment.userId()))
                                                .build())
                                        .collect(Collectors.toCollection(ArrayList::new)))
                                .build())
                        .collect(Collectors.toCollection(ArrayList::new)))
                .calculationProfile(originalForm)
                .overrideUserShiftRequestExceptNoDates(false)
                .overrideUserShiftRequestAll(false)
                .overrideShiftCountCap(false)
                .overrideConflictingDates(false)
                .overrideHasShiftRequest(false)
                .overridePreviousMonthValid(false)
                .build();
    }

    private UserCalculationData requireUser(Map<Long, User> usersById, Long userId) {

        UserCalculationData userCalculationData = userCalculationDataMapper.toCalculationData(usersById.get(userId), null);

        if (userCalculationData == null) {
            throw new IllegalStateException("Calculated schedule references missing user ID: " + userId);
        }
        return userCalculationData;
    }
}
