package com.richardbrenkus.shiftschedulermodernized.mapper;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.dto.form.*;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserStatViewRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.entity.UserStatEntity;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;

import java.util.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleMapper {

    private final UserRepository userRepository;

    public ScheduleEditForm toEditForm(ScheduleMonth scheduleMonth, CalculationProfileForm calculationProfileForm, List<Integer> shiftTypes) {
        ScheduleEditForm form = ScheduleEditForm.builder()
                .month(calculationProfileForm.getCalculationMonth())
                .shiftCountCap(calculationProfileForm.getShiftCountCap())
                .gapBetweenShifts(calculationProfileForm.getGapBetweenShifts())
                .sortByDatesAmount(calculationProfileForm.isSortByDatesAmount())
                .forceFillShiftTypes(new ArrayList<>(calculationProfileForm.getForceFillShiftTypes() == null? List.of() : calculationProfileForm.getForceFillShiftTypes()))
                .overrideUserShiftRequestExceptNoDates(scheduleMonth.isOverrideUserShiftRequestExceptNoDates())
                .overrideUserShiftRequestAll(scheduleMonth.isOverrideUserShiftRequestAll())
                .overrideShiftCountCap(scheduleMonth.isOverrideShiftCountCap())
                .overrideConflictingDates(scheduleMonth.isOverrideConflictingDates())
                .overrideHasShiftRequest(scheduleMonth.isOverrideHasShiftRequest())
                .overridePreviousMonthValid(scheduleMonth.isOverridePreviousMonthValid())
                .days(new ArrayList<>())
                .build();

        for (ScheduleDay scheduleDay : scheduleMonth.getDays()) {
            ScheduleDayForm dayForm = ScheduleDayForm.builder()
                    .date(scheduleDay.getDate())
                    .weekendOrHoliday(scheduleDay.isWeekendOrHoliday())
                    .assignments(new ArrayList<>())
                    .build();

            for (Integer shiftType : shiftTypes) {
                ShiftAssignment existingAssignment = scheduleDay.getAssignments()
                        .stream()
                        .filter(assignment -> assignment.getShiftType() == shiftType)
                        .findFirst()
                        .orElse(null);

                User assignedUser = existingAssignment == null ? null : existingAssignment.getUser();

                dayForm.getAssignments().add(ShiftAssignmentForm.builder()
                                .shiftType(shiftType)
                                .userId(assignedUser == null ? null : assignedUser.getId())
                                .name(assignedUser == null ? "" : assignedUser.getName())
                                .title(assignedUser == null ? "" : assignedUser.getTitle())
                                .build()
                );
            }

            form.getDays().add(dayForm);
        }

        return form;
    }

    @Transactional(readOnly = true)
    public ScheduleMonth toScheduleMonth(ScheduleEditForm form, CalculationProfileForm calculationProfile) {
        if (form == null) {
            throw new IllegalArgumentException("Schedule edit form must not be null");
        }

        List<ScheduleDay> days = form.getDays() == null
                ? new ArrayList<>()
                : form.getDays()
                .stream()
                .map(this::toScheduleDay)
                .toList();

        return ScheduleMonth.builder()
                .month(form.getMonth())
                .calculationProfile(calculationProfile)
                .days(new ArrayList<>(days))
                .overrideUserShiftRequestExceptNoDates(form.isOverrideUserShiftRequestExceptNoDates())
                .overrideUserShiftRequestAll(form.isOverrideUserShiftRequestAll())
                .overrideShiftCountCap(form.isOverrideShiftCountCap())
                .overrideConflictingDates(form.isOverrideConflictingDates())
                .overrideHasShiftRequest(form.isOverrideHasShiftRequest())
                .overridePreviousMonthValid(form.isOverridePreviousMonthValid())
                .build();
    }

    public UserStatEntity toUserStatEntity(UserStatViewRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("UserStatViewRecord must not be null");
        }

        Long userId = record.user() == null ? null : record.user().getId();
        String username = record.user() == null ? null : record.user().getUsername();

        return UserStatEntity.builder()
                .yearMonth(record.month())
                .userId(userId)
                .username(username)
                .name(record.name())
                .shiftType(record.shiftType())
                .requestedWeekdays(record.requestedWeekdays())
                .requestedWeekends(record.requestedWeekends())
                .calculatedWeekdays(record.calculatedWeekdays())
                .calculatedWeekends(record.calculatedWeekends())
                .remainingWeekdays(record.remainingWeekdays())
                .remainingWeekends(record.remainingWeekends())
                .anyDateSelected(record.anyDateSelected())
                .requestedDateDays(record.requestedDateDays())
                .assignedWeekdays(record.assignedWeekdays())
                .assignedWeekends(record.assignedWeekends())
                .assignedTotal(record.assignedTotal())
                .assignedTotalAllShiftTypes(record.assignedTotalAllShiftTypes())
                .assignedDateDays(record.assignedDateDays())
                .build();
    }

    public UserStatViewRecord toUserStatViewRecord(UserStatEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("UserStatEntity must not be null");
        }

        User user = entity.getUserId() == null
                ? null
                : userRepository.findById(entity.getUserId()).orElse(null);

        return UserStatViewRecord.builder()
                .user(user)
                .name(entity.getName())
                .shiftType(entity.getShiftType())
                .requestedWeekdays(entity.getRequestedWeekdays())
                .requestedWeekends(entity.getRequestedWeekends())
                .calculatedWeekdays(entity.getCalculatedWeekdays())
                .calculatedWeekends(entity.getCalculatedWeekends())
                .remainingWeekdays(entity.getRemainingWeekdays())
                .remainingWeekends(entity.getRemainingWeekends())
                .anyDateSelected(entity.isAnyDateSelected())
                .requestedDateDays(entity.getRequestedDateDays())
                .assignedWeekdays(entity.getAssignedWeekdays())
                .assignedWeekends(entity.getAssignedWeekends())
                .assignedTotal(entity.getAssignedTotal())
                .assignedTotalAllShiftTypes(entity.getAssignedTotalAllShiftTypes())
                .assignedDateDays(entity.getAssignedDateDays())
                .month(entity.getYearMonth())
                .build();
    }

    private ScheduleDay toScheduleDay(ScheduleDayForm form) {
        List<ShiftAssignment> assignments = form.getAssignments() == null
                ? new ArrayList<>()
                : form.getAssignments()
                .stream()
                .map(this::toShiftAssignment)
                .filter(Objects::nonNull)
                .toList();

        return ScheduleDay.builder()
                .date(form.getDate())
                .weekendOrHoliday(form.isWeekendOrHoliday())
                .assignments(new ArrayList<>(assignments))
                .build();
    }

    private ShiftAssignment toShiftAssignment(ShiftAssignmentForm form) {
        if (form == null) {
            return null;
        }

        User user = form.getUserId() == null
                ? null
                : userRepository.findById(form.getUserId()).orElse(null);

        return ShiftAssignment.builder()
                .shiftType(form.getShiftType())
                .user(user)
                .build();
    }
}


