package com.richardbrenkus.shiftschedulermodernized.mapper;

import com.richardbrenkus.shiftschedulermodernized.algorithm.CalendarDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleCalendar;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.dto.form.*;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScheduleMapper {

    private final UserRepository userRepository;

    public ScheduleEditForm toEditForm(
            ScheduleCalendar calendar,
            CalculationProfileForm calculationProfileForm
    ) {
        ScheduleEditForm form = ScheduleEditForm.builder()
                .month(calculationProfileForm.getCalculationMonth())
                .shiftCountCap(calculationProfileForm.getShiftCountCap())
                .gapBetweenShifts(calculationProfileForm.getGapBetweenShifts())
                .sortByDatesAmount(calculationProfileForm.isSortByDatesAmount())
                .forceFillShiftTypes(new ArrayList<>(
                        calculationProfileForm.getForceFillShiftTypes() == null
                                ? List.of()
                                : calculationProfileForm.getForceFillShiftTypes()
                ))
                .overrideUserShiftRequestExceptNoDates(calendar.isOverrideUserShiftRequestExceptNoDates())
                .overrideUserShiftRequestAll(calendar.isOverrideUserShiftRequestAll())
                .overrideShiftCountCap(calendar.isOverrideShiftCountCap())
                .overrideConflictingDates(calendar.isOverrideConflictingDates())
                .overrideHasShiftRequest(calendar.isOverrideHasShiftRequest())
                .overridePreviousMonthValid(calendar.isOverridePreviousMonthValid())
                .build();

        List<ScheduleDayForm> days = calendar.getDays()
                .stream()
                .map(day -> ScheduleDayForm.builder()
                        .date(day.getDate())
                        .weekendOrHoliday(day.isWeekendOrHoliday())
                        .assignments(day.getAssignments()
                                .stream()
                                .map(assignment -> ShiftAssignmentForm.builder()
                                        .shiftType(assignment.getShiftType())
                                        .userId(assignment.getUser() == null ? null : assignment.getUser().getId())
                                        .name(assignment.getUser() == null ? "" : assignment.getUser().getName())
                                        .title(assignment.getUser() == null ? "" : assignment.getUser().getTitle())
                                        .build())
                                .toList())
                        .build())
                .toList();

        form.setDays(new ArrayList<>(days));

        return form;
    }

    private ScheduleDayForm toDayForm(CalendarDay day) {
        List<ShiftAssignmentForm> assignmentForms = day.getAssignments() == null
                ? new ArrayList<>()
                : day.getAssignments()
                .stream()
                .sorted(Comparator.comparingInt(ShiftAssignment::getShiftType))
                .map(this::toAssignmentForm)
                .toList();

        return ScheduleDayForm.builder()
                .date(day.getDate())
                .weekendOrHoliday(day.isWeekendOrHoliday())
                .assignments(new ArrayList<>(assignmentForms))
                .build();
    }

    private ShiftAssignmentForm toAssignmentForm(ShiftAssignment assignment) {
        return ShiftAssignmentForm.builder()
                .shiftType(assignment.getShiftType())
                .userId(assignment.getUser() == null ? null : assignment.getUser().getId())
                .build();
    }

    @Transactional(readOnly = true)
    public ScheduleCalendar toScheduleCalendar(
            ScheduleEditForm form,
            CalculationProfileForm calculationProfile
    ) {
        if (form == null) {
            throw new IllegalArgumentException("Schedule edit form must not be null");
        }

        List<CalendarDay> days = form.getDays() == null
                ? new ArrayList<>()
                : form.getDays()
                .stream()
                .map(this::toCalendarDay)
                .toList();

        return ScheduleCalendar.builder()
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

    private CalendarDay toCalendarDay(ScheduleDayForm form) {
        List<ShiftAssignment> assignments = form.getAssignments() == null
                ? new ArrayList<>()
                : form.getAssignments()
                .stream()
                .map(this::toShiftAssignment)
                .filter(Objects::nonNull)
                .toList();

        return CalendarDay.builder()
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


