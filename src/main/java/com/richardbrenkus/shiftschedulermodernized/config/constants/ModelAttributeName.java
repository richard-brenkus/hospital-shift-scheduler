package com.richardbrenkus.shiftschedulermodernized.config.constants;

public final class ModelAttributeName {

    private ModelAttributeName() {
    }

    // Scheduled tasks
    public static final String REMINDER_TASK_INFO = "reminderTaskInfo";
    public static final String CLEANUP_TASK_INFO = "cleanupTaskInfo";
    public static final String REMINDER_IS_ACTIVE = "reminderIsActive";
    public static final String CLEANUP_IS_ACTIVE = "cleanupIsActive";
    public static final String REMINDER_REPETITIONS = "reminderRepetitions";
    public static final String REMINDER_FREQUENCY = "reminderFrequency";
    public static final String CLEANUP_DATE_TIME = "cleanupDateTime";
    public static final String REMINDER_START = "reminderStart";
    public static final String REMINDER_DEADLINE = "reminderDeadline";
    public static final String REMINDER_TASK = "reminderTask";
    public static final String CLEANUP_TASK = "cleanupTask";

    // User
    public static final String DISPLAY_NAME = "displayName";
    public static final String USERNAME_PASSED = "usernamePassed";
    public static final String IS_ADMIN = "isAdmin";
    public static final String USER_COUNT = "userCount";
    public static final String SHIFT_REQUEST_COUNT = "shiftRequestCount";
    public static final String PERCENTAGE = "percentage";
    public static final String HAS_SHIFT_REQUEST = "hasShiftRequest";
    public static final String PASSWORD_CHANGE_FORM = "passwordChangeForm";
    public static final String SHIFT_TYPES = "shiftTypes";
    public static final String USERS = "users";
    public static final String UPDATED_USER = "updatedUser";
    public static final String USERNAME = "username";
    public static final String SELECTED_USER = "selectedUser";

    // Shift request
    public static final String SHIFT_REQUEST_FORM = "shiftRequestForm";
    public static final String SUBMITTED_SHIFT_REQUEST_RECORD = "submittedShiftRequestRecord";
    public static final String CURRENT_PRIORITY = "currentPriority";


    // Selection lists
    public static final String WEEKDAY_COUNT_LIST = "weekdayCountList";
    public static final String WEEKEND_COUNT_LIST = "weekendCountList";
    public static final String PRIORITY_LIST = "priorityList";

    // Validation flags
    public static final String CONFLICTING_DATES = "conflictingDates";
    public static final String SHIFT_AND_WEEKEND_COUNT = "shiftAndWeekendCount";
    public static final String INVALID_INPUT_CONDITION_1 = "invalidInputCondition1";
    public static final String INVALID_INPUT_CONDITION_2 = "invalidInputCondition2";
    public static final String NO_SHIFTS_ONLY_SELECTED = "noShiftsOnlySelected";
    public static final String NO_DATES_ONLY = "noDatesOnly";
    public static final String YES_DATES_ANY_DATE = "yesDatesAnyDate";

    // Validation errors
    public static final String NO_MATCH = "noMatch";
    public static final String TOO_SHORT = "tooShort";


    // Other
    public static final String USER_REGISTER_FORM = "userRegisterForm";
    public static final String USER_UPDATE_FORM = "userUpdateForm";

    public static final String PROFESSIONS = "professions";
    public static final String REDIRECT_URL = "redirectUrl";
}