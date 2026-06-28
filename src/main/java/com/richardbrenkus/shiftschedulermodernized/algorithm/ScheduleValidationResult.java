package com.richardbrenkus.shiftschedulermodernized.algorithm;

import lombok.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleValidationResult {

    private ScheduleCalendar calendar;

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

    @Builder.Default
    private Set<String> nonExistentUsers = new HashSet<>();

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
    private Map<Integer, Set<UserStat>> shortStatsByShiftType = new HashMap<>();

    @Builder.Default
    private Map<Integer, Set<UserStat>> noShiftAssignedStatsByShiftType = new HashMap<>();

    @Builder.Default
    private Map<Integer, Set<UserStat>> fullUserStatsByShiftType = new HashMap<>();

    public void markRedField(int shiftType, int dayOfMonth) {
        redFieldsByShiftType
                .computeIfAbsent(shiftType, key -> new HashSet<>())
                .add(dayOfMonth);
    }

    public void addShiftCapUser(int shiftType, String userName) {
        shiftCapUsersByShiftType
                .computeIfAbsent(shiftType, key -> new HashSet<>())
                .add(userName);
    }

    public void addIndividualShiftCapUser(int shiftType, String userName) {
        individualShiftCapUsersByShiftType
                .computeIfAbsent(shiftType, key -> new HashSet<>())
                .add(userName);
    }

    public void addWeekendCapUser(int shiftType, String userName) {
        weekendCapUsersByShiftType
                .computeIfAbsent(shiftType, key -> new HashSet<>())
                .add(userName);
    }

    public void addCrossCheckUser(int shiftType, String userName) {
        crossCheckUsersByShiftType
                .computeIfAbsent(shiftType, key -> new HashSet<>())
                .add(userName);
    }

    public void addUserNoRequest(int shiftType, String userName) {
        userNoRequestByShiftType
                .computeIfAbsent(shiftType, key -> new HashSet<>())
                .add(userName);
    }

    public void addDatesNoCheckUser(int shiftType, String userName) {
        datesNoCheckUsersByShiftType
                .computeIfAbsent(shiftType, key -> new HashSet<>())
                .add(userName);
    }

    public void addPreviousMonthCheckUser(int shiftType, String userName) {
        previousMonthCheckUsersByShiftType
                .computeIfAbsent(shiftType, key -> new HashSet<>())
                .add(userName);
    }
}
