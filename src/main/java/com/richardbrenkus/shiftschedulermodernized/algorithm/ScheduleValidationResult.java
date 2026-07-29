package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.dto.view.UserStatViewRecord;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleValidationResult {

    private ScheduleMonth scheduleMonth;

    private boolean errorsExist;
    private boolean allUsersExist;

    private boolean userNonexistent;
    private boolean userShiftCap;
    private boolean userIndividualShiftCap;
    private boolean userWeekendCap;
    private boolean userCrossCheck;
    private boolean userNoRequest;
    private boolean userDatesNo;
    private boolean previousMonthCheckFailed;

    private boolean shortStatsExist;
    private boolean noShiftAssignedStatsExist;

    private String scheduleScore;
    private String usersWithNoRequestString;

    @Builder.Default
    private Set<String> nonExistentUsers = new TreeSet<>();

    @Builder.Default
    private Map<Integer, Set<Integer>> redFieldsByShiftType = new HashMap<>();

    @Builder.Default
    private Map<Integer, Set<String>> shiftCapUsersByShiftType = new HashMap<>();

    @Builder.Default
    private Map<Integer, Set<String>> individualShiftCapUsersByShiftType = new HashMap<>();

    @Builder.Default
    private Map<Integer, Set<String>> weekendCapUsersByShiftType = new HashMap<>();

    @Builder.Default
    private Map<Integer, Set<String>> crossCheckUsersByShiftType = new HashMap<>();

    @Builder.Default
    private Map<Integer, Set<String>> userNoRequestByShiftType = new HashMap<>();

    @Builder.Default
    private Map<Integer, Set<String>> datesNoCheckUsersByShiftType = new HashMap<>();

    @Builder.Default
    private Map<Integer, Set<String>> previousMonthCheckUsersByShiftType = new HashMap<>();

    @Builder.Default
    private Map<Integer, Set<UserStatViewRecord>> shortStatsByShiftType = new HashMap<>();

    @Builder.Default
    private Map<Integer, Set<UserStatViewRecord>> noShiftAssignedStatsByShiftType = new HashMap<>();

    @Builder.Default
    private Map<Integer, Set<UserStatViewRecord>> fullUserStatsByShiftType = new HashMap<>();

    public void markRedField(int shiftType, int dayOfMonth) {
        redFieldsByShiftType.computeIfAbsent(shiftType, key -> new HashSet<>()).add(dayOfMonth);
    }

    public void addShiftCapUser(int shiftType, String userName) {
        addName(shiftCapUsersByShiftType, shiftType, userName);
    }

    public void addIndividualShiftCapUser(int shiftType, String userName) {
        addName(individualShiftCapUsersByShiftType, shiftType, userName);
    }

    public void addWeekendCapUser(int shiftType, String userName) {
        addName(weekendCapUsersByShiftType, shiftType, userName);
    }

    public void addCrossCheckUser(int shiftType, String userName) {
        addName(crossCheckUsersByShiftType, shiftType, userName);
    }

    public void addUserNoRequest(int shiftType, String userName) {
        addName(userNoRequestByShiftType, shiftType, userName);
    }

    public void addDatesNoCheckUser(int shiftType, String userName) {
        addName(datesNoCheckUsersByShiftType, shiftType, userName);
    }

    public void addPreviousMonthCheckUser(int shiftType, String userName) {
        addName(previousMonthCheckUsersByShiftType, shiftType, userName);
    }

    private void addName(Map<Integer, Set<String>> target, int shiftType, String userName) {
        if (userName == null || userName.isBlank()) {
            return;
        }

        target.computeIfAbsent(shiftType, key -> new TreeSet<>()).add(userName);
    }
}

