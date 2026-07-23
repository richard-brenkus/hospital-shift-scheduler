package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculatedShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ScheduleCandidate;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
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

    @Transactional(readOnly = true)
    public ScheduleMonth toLegacyScheduleMonth(
            ScheduleCandidate candidate,
            CalculationProfileForm originalForm
    ) {
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
                                                .user(requireUser(usersById, assignment.userId()))
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

    private User requireUser(Map<Long, User> usersById, Long userId) {
        User user = usersById.get(userId);
        if (user == null) {
            throw new IllegalStateException("Calculated schedule references missing user ID: " + userId);
        }
        return user;
    }
}
