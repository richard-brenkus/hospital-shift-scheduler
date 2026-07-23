package com.richardbrenkus.shiftschedulermodernized.mapper;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ShiftAssignment;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ScheduleDayForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ScheduleEditForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftAssignmentForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserStatViewRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.entity.UserStatEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScheduleMapper {

    public ScheduleEditForm toEditForm(ScheduleMonth scheduleMonth, CalculationProfileForm calculationProfileForm, List<Integer> shiftTypes) {
        Objects.requireNonNull(scheduleMonth, "scheduleMonth must not be null");
        Objects.requireNonNull(calculationProfileForm, "calculationProfileForm must not be null");
        Objects.requireNonNull(shiftTypes, "shiftTypes must not be null");
        Objects.requireNonNull(scheduleMonth.getMonth(), "scheduleMonth.month must not be null");
        Objects.requireNonNull(scheduleMonth.getCalculationProfile(), "scheduleMonth.calculationProfile must not be null");
        Objects.requireNonNull(calculationProfileForm.getCalculationMonth(), "calculationProfileForm.calculationMonth must not be null");
        if(!Objects.equals(scheduleMonth.getMonth(), calculationProfileForm.getCalculationMonth()))
            throw new IllegalArgumentException("scheduleMonth.month must equal calculationProfileForm.calculationMonth");

        List<ScheduleDay> scheduleDays = Objects.requireNonNull(scheduleMonth.getDays(), "scheduleMonth.days must not be null");

        ScheduleEditForm form = ScheduleEditForm.builder()
                .month(calculationProfileForm.getCalculationMonth())
                .shiftCountCap(calculationProfileForm.getShiftCountCap())
                .gapBetweenShifts(calculationProfileForm.getGapBetweenShifts())
                .sortByDatesAmount(calculationProfileForm.isSortByDatesAmount())
                .forceFillShiftTypes(new ArrayList<>(calculationProfileForm.getForceFillShiftTypes() == null ? List.of() : calculationProfileForm.getForceFillShiftTypes()))
                .overrideUserShiftRequestExceptNoDates(scheduleMonth.isOverrideUserShiftRequestExceptNoDates())
                .overrideUserShiftRequestAll(scheduleMonth.isOverrideUserShiftRequestAll())
                .overrideShiftCountCap(scheduleMonth.isOverrideShiftCountCap())
                .overrideConflictingDates(scheduleMonth.isOverrideConflictingDates())
                .overrideHasShiftRequest(scheduleMonth.isOverrideHasShiftRequest())
                .overridePreviousMonthValid(scheduleMonth.isOverridePreviousMonthValid())
                .days(new ArrayList<>())
                .build();

        for (ScheduleDay scheduleDay : scheduleDays) {
            Objects.requireNonNull(scheduleDay,"scheduleMonth.days must not contain null entries");

            List<ShiftAssignment> assignments = scheduleDay.getAssignments() == null ? List.of() : scheduleDay.getAssignments();

            Map<Integer, ShiftAssignment> assignmentsByType = assignments.stream()
                    .collect(Collectors.toMap(ShiftAssignment::getShiftType,Function.identity(),(a, b) -> {throw new IllegalStateException("Duplicate shift type " + a.getShiftType());}));

            ScheduleDayForm dayForm = ScheduleDayForm.builder()
                    .date(scheduleDay.getDate())
                    .weekendOrHoliday(scheduleDay.isWeekendOrHoliday())
                    .assignments(new ArrayList<>())
                    .build();

            for (Integer shiftType : shiftTypes) {
                Objects.requireNonNull(shiftType, "shiftTypes must not contain null entries");

                ShiftAssignment existingAssignment = assignmentsByType.get(shiftType);

                User assignedUser = existingAssignment == null ? null : existingAssignment.getUser();

                dayForm.getAssignments().add(ShiftAssignmentForm.builder()
                                .shiftType(shiftType)
                                .userId(assignedUser == null ? null : assignedUser.getId())
                                .name(assignedUser == null ? "" : assignedUser.getName())
                                .title(assignedUser == null ? "" : assignedUser.getTitle())
                                .build());
            }

            form.getDays().add(dayForm);
        }

        return form;
    }

    public ScheduleMonth toScheduleMonth(ScheduleEditForm form, CalculationProfileForm calculationProfile, Map<Long, User> usersById) {
        Objects.requireNonNull(form,"ScheduleEditForm must not be null");
        Objects.requireNonNull(calculationProfile,"calculationProfile must not be null");
        Objects.requireNonNull(form.getDays(),"form.days must not be null");
        Objects.requireNonNull(calculationProfile.getCalculationMonth(),"calculationProfile.calculationMonth must not be null");
        Objects.requireNonNull(usersById,"usersById must not be null");
        Objects.requireNonNull(form.getMonth(),"form.month must not be null");
        Objects.requireNonNull(calculationProfile.getCalculationMonth(),"calculationProfile.calculationMonth must not be null");
        if(Objects.equals(form.getMonth(),calculationProfile.getCalculationMonth()))
            throw new IllegalArgumentException("form.month must equal calculationProfile.calculationMonth");

        List<ScheduleDay> days = new ArrayList<>();
        for(ScheduleDayForm dayForm : form.getDays())
            days.add(toScheduleDay(dayForm, usersById));

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
        Objects.requireNonNull(record,"UserStatViewRecord must not be null");

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
                .assignedTotalAllShiftTypes(
                        record.assignedTotalAllShiftTypes()
                )
                .assignedDateDays(record.assignedDateDays())
                .build();
    }

    public UserStatViewRecord toUserStatViewRecord(UserStatEntity entity, Map<Long, User> usersById) {
        Objects.requireNonNull(entity,"UserStatEntity must not be null");
        Objects.requireNonNull(usersById,"usersById must not be null");

        User user = entity.getUserId() == null ? null : usersById.get(entity.getUserId());

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
                .assignedTotalAllShiftTypes(
                        entity.getAssignedTotalAllShiftTypes()
                )
                .assignedDateDays(entity.getAssignedDateDays())
                .month(entity.getYearMonth())
                .build();
    }

    private ScheduleDay toScheduleDay(ScheduleDayForm form, Map<Long, User> usersById) {
        Objects.requireNonNull(form, "ScheduleDayForm must not be null");

        List<ShiftAssignment> assignments = new ArrayList<>();
        for(ShiftAssignmentForm assignmentForm : form.getAssignments())
            assignments.add(toShiftAssignment(assignmentForm, usersById));

        return ScheduleDay.builder()
                .date(form.getDate())
                .weekendOrHoliday(form.isWeekendOrHoliday())
                .assignments(new ArrayList<>(assignments))
                .build();
    }

    private ShiftAssignment toShiftAssignment(ShiftAssignmentForm form, Map<Long, User> usersById) {
        Objects.requireNonNull(form,"ShiftAssignmentForm must not be null");
        Objects.requireNonNull(usersById,"usersById must not be null");

        User user = null;

        if (form.getUserId() != null) {
            user = usersById.get(form.getUserId());
            if (user == null) {
                throw new IllegalArgumentException("No user exists for submitted assignment user ID " + form.getUserId());
            }
        }

        return ShiftAssignment.builder()
                .shiftType(form.getShiftType())
                .user(user)
                .build();
    }
}