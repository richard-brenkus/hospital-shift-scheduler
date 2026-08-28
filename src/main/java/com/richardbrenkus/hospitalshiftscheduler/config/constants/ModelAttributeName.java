package com.richardbrenkus.hospitalshiftscheduler.config.constants;

public final class ModelAttributeName {

    private ModelAttributeName() {
    }

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
    public static final String CALCULATION_PROFILE_FORM = "calculationProfileForm";
    public static final String MONTH_OPTIONS = "monthOptions";
    public static final String GAP_BETWEEN_SHIFTS = "gapBetweenShiftsList";
    public static final String SHIFT_COUNT_MAX = "shiftCountMaxList";
    public static final String DAYS_LIST = "daysList";
    public static final String HOURS_LIST = "hoursList";
    public static final String MINUTES_LIST = "minutesList";
    public static final String REPETITIONS_LIST = "repetitionsList";
    public static final String FREQUENCY_LIST = "frequencyList";
    public static final String FINAL_SUBMISSION_DAYS_LIST = "finalSubmissionDaysList";

    // Validation flags
    public static final String CONFLICTING_DATES = "conflictingDates";
    public static final String CALCULATION_ERROR_CODE = "calculationErrorCode";
    public static final String SCHEDULE_EXISTS = "scheduleExists";
    public static final String SCHEDULE_VALIDATION_RESULT = "scheduleValidationResult";

    // Schedule
    public static final String SHOW_EXISTING_SCHEDULE_CONFIRMATION = "showExistingScheduleConfirmation";
    public static final String EXISTING_SCHEDULE_MONTH = "existingScheduleMonth";
    public static final String FULL_USER_STATS_BY_SHIFT_TYPE = "fullUserStatsByShiftType";
    public static final String STATS_EXIST = "statsExist";
    public static final String MONTH = "month";
    public static final String YEAR = "year";
    public static final String MONTH_INT = "monthInt";
    public static final String SCHEDULE_EDIT_FORM = "scheduleEditForm";
    public static final String SHOW_EXISTING_SCHEDULE_SAVE_CONFIRMATION = "showExistingScheduleSaveConfirmation";
    public static final String SAVED_SCHEDULE = "savedSchedule";
    public static final String MONTH_DAYS_LIST = "monthDaysList";
    public static final String WEEKENDS_AND_HOLIDAYS = "weekendsAndHolidays";
    public static final String SAVED_SCHEDULE_SELECTION_FORM = "savedScheduleSelectionForm";
    public static final String USERS_WITH_NO_REQUEST = "usersWithNoRequest";
    public static final String USERS_WITH_NO_REQUEST_STRING = "usersWithNoRequestString";

    // Scheduled tasks
    public static final String CLEANUP_TASK_RECORD = "cleanupTaskRecord";
    public static final String SEND_REMINDER_TASK_RECORD = "sendReminderTaskRecord";
    public static final String CLEANUP_TASK_FORM = "cleanupTaskForm";
    public static final String SEND_REMINDER_TASK_FORM = "sendReminderTaskForm";

    // Other
    public static final String USER_REGISTER_FORM = "userRegisterForm";
    public static final String USER_UPDATE_FORM = "userUpdateForm";
    public static final String ACTION_TYPE = "actionType";
    public static final String PROFESSIONS = "professions";
    public static final String REDIRECT_URL = "redirectUrl";
}
