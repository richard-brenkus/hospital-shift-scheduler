package com.richardbrenkus.shiftschedulermodernized.config.constants;

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

    // Validation flags
    public static final String CONFLICTING_DATES = "conflictingDates";
    public static final String NO_MATCH = "noMatch";
    public static final String TOO_SHORT = "tooShort";


    // Other
    public static final String USER_REGISTER_FORM = "userRegisterForm";
    public static final String USER_UPDATE_FORM = "userUpdateForm";

    public static final String ACTION_TYPE = "actionType";

    public static final String PROFESSIONS = "professions";
    public static final String REDIRECT_URL = "redirectUrl";
}