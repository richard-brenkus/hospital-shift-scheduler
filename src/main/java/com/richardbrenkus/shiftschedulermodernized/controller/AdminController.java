package com.richardbrenkus.shiftschedulermodernized.controller;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleCalendar;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleValidationResult;
import com.richardbrenkus.shiftschedulermodernized.config.ApplicationConstants;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ModelAttributeName;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Profession;
import com.richardbrenkus.shiftschedulermodernized.dto.form.*;
import com.richardbrenkus.shiftschedulermodernized.dto.view.*;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ScheduleMapper;
import com.richardbrenkus.shiftschedulermodernized.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final LandingPageService landingPageService;
    private final UserService userService;
    private final ScheduledTasksService scheduledTasksService;
    private final ShiftTypeService shiftTypeService;
    private final ShiftRequestService shiftRequestService;
    private final PrepareModelService prepareModelService;
    private final CalculationProfileService calculationProfileService;
    private final ScheduleCalculationService scheduleCalculationService;
    private final ScheduleValidationService scheduleValidationService;
    private final ScheduleMapper scheduleMapper;

    @GetMapping("/admin/adminIndex")
    public String adminIndex(Authentication authentication, Model model) {

        String username = authentication.getName();

        model.addAttribute(ModelAttributeName.DISPLAY_NAME, userService.getDisplayNameByUserName(username));

        LandingPageRecord landingPageRecord = landingPageService.getLandingPageRecord();
        model.addAttribute(ModelAttributeName.USER_COUNT, landingPageRecord.userCountWithoutAdmin());
        model.addAttribute(ModelAttributeName.SHIFT_REQUEST_COUNT, landingPageRecord.shiftRequestCount());
        model.addAttribute(ModelAttributeName.PERCENTAGE, landingPageRecord.percentage());

        ScheduledTasksRecord scheduledTasksRecord = scheduledTasksService.getScheduledTasksRecord();
        model.addAttribute(ModelAttributeName.REMINDER_DEADLINE, scheduledTasksRecord.reminderDeadline());
        model.addAttribute(ModelAttributeName.REMINDER_IS_ACTIVE, scheduledTasksRecord.reminderIsActive());
        model.addAttribute(ModelAttributeName.REMINDER_TASK_INFO, scheduledTasksRecord.reminderTaskInfo());
        model.addAttribute(ModelAttributeName.REMINDER_TASK, scheduledTasksRecord.reminderTask());
        model.addAttribute(ModelAttributeName.REMINDER_START, scheduledTasksRecord.reminderStart());
        model.addAttribute(ModelAttributeName.REMINDER_FREQUENCY, scheduledTasksRecord.reminderFrequency());
        model.addAttribute(ModelAttributeName.REMINDER_REPETITIONS, scheduledTasksRecord.reminderRepetitions());
        model.addAttribute(ModelAttributeName.CLEANUP_TASK_INFO, scheduledTasksRecord.cleanupTaskInfo());
        model.addAttribute(ModelAttributeName.CLEANUP_IS_ACTIVE, scheduledTasksRecord.cleanupIsActive());
        model.addAttribute(ModelAttributeName.CLEANUP_DATE_TIME, scheduledTasksRecord.cleanupDateTime());
        model.addAttribute(ModelAttributeName.CLEANUP_TASK, scheduledTasksRecord.cleanupTask());

        return "admin/adminIndex";
    }

    @GetMapping("/admin/register_user")
    public String showRegistrationForm(Model model) {
        model.addAttribute(ModelAttributeName.USER_REGISTER_FORM, new UserRegisterForm());
        model.addAttribute(ModelAttributeName.PROFESSIONS, Profession.values());
        model.addAttribute(ModelAttributeName.SHIFT_TYPES, shiftTypeService.getShiftTypes());
        //model.addAttribute(ModelAttributeName.ACTION_TYPE, ModelAttributeValue.ACTION_TYPE_ADD);
        //model.addAttribute(ModelAttributeName.HEADER_TYPE, ModelAttributeValue.HEADER_TYPE_ADMIN_ADD);

        return "admin/register_user";
    }

    @PostMapping(path = "/admin/add")
    public String addNewUser(@Valid @ModelAttribute(ModelAttributeName.USER_REGISTER_FORM) UserRegisterForm userRegisterForm,
                             BindingResult bindingResult,
                             Model model) {

        if (userRegisterForm.getAllowedShiftTypes() == null || userRegisterForm.getAllowedShiftTypes().isEmpty()) {
            bindingResult.rejectValue(
                    "allowedShiftTypes",
                    "error.allowedShiftTypes",
                    "Please select at least one shift type!"
            );
        }

        if (userService.existsByUsernameIgnoreCase(userRegisterForm.getUsername())) {
            bindingResult.rejectValue(
                    "username",
                    "error.username",
                    "This user name has already been used!"
            );
        }

        if (userService.existsByEmailIgnoreCase(userRegisterForm.getEmail())) {
            bindingResult.rejectValue(
                    "email",
                    "error.email",
                    "This email address has already been used!"
            );
        }

        if (userService.existsByNameIgnoreCase(userRegisterForm.getName())) {
            bindingResult.rejectValue(
                    "name",
                    "error.name",
                    "A user with the same name already exists! Please make the name unique."
            );
        }

        if (bindingResult.hasErrors()) {
            prepareModelService.prepareRegisterUserModel(model);
            return "admin/register_user";
        }

        userService.createUser(userRegisterForm);

        //model.addAttribute(ModelAttributeName.ACTION_TYPE, ModelAttributeValue.ACTION_TYPE_ADD);
        return "admin/user_update_success";
    }

    @GetMapping("/admin/delete_user")
    public String showDeleteUserPage(Model model) {
        List<User> users = userService.getAllUsersWithoutAdminByNameAsc();

        model.addAttribute(ModelAttributeName.USERS, users);
        return "admin/delete_user";
    }

    @PostMapping("/admin/delete_user")
    public String deleteUser(@RequestParam Long userId) {
        User user = userService.getUserById(userId);

        if (user.isAdmin()) {
            throw new IllegalArgumentException("Admin user cannot be deleted.");
        }

        userService.deleteUser(user);

        return "admin/user_delete_success";
    }

    @GetMapping(path = "/admin/delete_shift_request")
    public String deleteRequestSelect(Model model) {

        final UserRegisterForm updatedUser = new UserRegisterForm();
        List<User> usersList = userService.getAllUsersWithShiftRequestByNameAsc();

        model.addAttribute(ModelAttributeName.USERS, usersList);
        model.addAttribute(ModelAttributeName.UPDATED_USER, updatedUser);

        return "admin/delete_shift_request";

    }

    @PostMapping(path = "/admin/delete_request")
    public String deleteRequest(@RequestParam(name = "id") long userId) {

        shiftRequestService.deleteShiftRequest(userId);

        return "redirect:/admin/adminIndex";
    }

    @GetMapping(path = "/admin/update_user")
    public String updateUser(Model model) {

        final UserRegisterForm updatedUser = new UserRegisterForm();
        List<User> usersList = userService.getAllUsersAndAdminsByNameAsc();

        model.addAttribute(ModelAttributeName.USERS, usersList);
        model.addAttribute(ModelAttributeName.UPDATED_USER, updatedUser);

        return "admin/select_update_user";
    }

    @GetMapping(path = "/admin/update_shift_request")
    public String updateRequest(Model model) {

        final User updatedUser = new User();
        List<User> usersList = userService.getAllUsersWithShiftRequestByNameAsc();

        model.addAttribute(ModelAttributeName.USERS, usersList);
        model.addAttribute(ModelAttributeName.UPDATED_USER, updatedUser);

        return "admin/update_shift_request";
    }

    @PostMapping(path = "/admin/update_shift_request")
    public String updateShiftRequest(Model model, @RequestParam long id) {

        ShiftRequestForm shiftRequestForm = shiftRequestService.getShiftRequestFormByUserId(id);
        prepareModelService.populateUserIndexModelForAdmin(model, shiftRequestForm, true, id);

        return "user/userIndex";
    }

    @PostMapping(path = "/admin/select_update_user")
    public String getUserUpdateForm(@ModelAttribute("user") User user, @RequestParam long id, Model model) {

        final UserUpdateForm updatedUser = userService.getUserUpdateFormByUserId(id);

        model.addAttribute(ModelAttributeName.USER_UPDATE_FORM, updatedUser);
        model.addAttribute(ModelAttributeName.PROFESSIONS, Profession.values());
        model.addAttribute(ModelAttributeName.SHIFT_TYPES, shiftTypeService.getShiftTypes());
        //model.addAttribute(ModelAttributeName.ACTION_TYPE, ModelAttributeValue.ACTION_TYPE_UPDATE);
        //model.addAttribute(ModelAttributeName.HEADER_TYPE, ModelAttributeValue.HEADER_TYPE_ADMIN_UPDATE);

        return "admin/update_user";
    }

    @PostMapping(path = "/admin/update_user")
    public String postUserUpdateForm(
            @Valid @ModelAttribute(ModelAttributeName.USER_UPDATE_FORM) UserUpdateForm updatedUser,
            BindingResult bindingResult,
            Model model) {

        prepareModelService.prepareUpdateUserModel(model);
        UserUpdateValidationResult userUpdateValidationResult = userService.validateUserUpdate(updatedUser);
        if (!userUpdateValidationResult.isValid()) {
            for (int i = 0; i < userUpdateValidationResult.rejectedFields().size(); i++) {
                String rejectedField = userUpdateValidationResult.rejectedFields().get(i);
                String errorCode = "error." + rejectedField;
                String defaultMessage = userUpdateValidationResult.defaultMessages().get(i);

                bindingResult.rejectValue(rejectedField, errorCode, defaultMessage);
            }
        }

        if (bindingResult.hasErrors()) {
            return "admin/update_user";
        }

        userService.updateUser(updatedUser);

        return "admin/user_update_success";
    }

    @GetMapping("/admin/all_users_list")
    public String showUsers(Model model) {

        List<UserViewRecord> usersList = userService.getAllUserSummaryViewRecordsByNameAsc();
        model.addAttribute(ModelAttributeName.IS_ADMIN, true);
        model.addAttribute(ModelAttributeName.USERS, usersList);
        return "admin/all_users_list";
    }

    @PostMapping("/admin/show_request_summary")
    public String showRequestSummaryForAdmin(@RequestParam Long userId,
                                             Model model) {

        User user = userService.getUserById(userId);
        ShiftRequestViewRecord record = shiftRequestService.getShiftRequestViewRecord(user.getUsername()).orElse(null);

        model.addAttribute(ModelAttributeName.DISPLAY_NAME, userService.getDisplayNameByUserName(user.getUsername()));
        model.addAttribute(ModelAttributeName.SUBMITTED_SHIFT_REQUEST_RECORD, record);
        model.addAttribute(ModelAttributeName.IS_ADMIN, true);

        return "user/shift_request_summary";
    }

    @GetMapping("/admin/select_user_password_change")
    public String showSelectUserPasswordChangePage(Model model) {

        List<UserViewRecord> users = userService.findAllUsersForSelectionByNameAsc();

        model.addAttribute(ModelAttributeName.USERS, users);
        model.addAttribute(ModelAttributeName.SELECTED_USER, UserViewRecord.builder().build());

        return "admin/select_user_password_change";
    }

    @PostMapping("admin/select_user_password_change")
    public String receiveSelectedUserForPasswordChange(
            @RequestParam Long userId,
            Model model
    ) {
        UserViewRecord user = userService.findUserViewById(userId);

        model.addAttribute(ModelAttributeName.SELECTED_USER, user);
        model.addAttribute(ModelAttributeName.PASSWORD_CHANGE_FORM, new PasswordChangeForm());

        return "admin/change_user_password";
    }

    @PostMapping("/admin/change_user_password")
    public String changeUserPasswordByAdmin(
            @RequestParam("userId") Long userId,
            @Valid @ModelAttribute("passwordChangeForm") PasswordChangeForm passwordChangeForm,
            BindingResult bindingResult,
            Model model
    ) {

        UserViewRecord selectedUser = userService.findUserViewById(userId);

        if (!passwordChangeForm.passwordsMatch()) {
            bindingResult.rejectValue(
                    "confirmedPassword",
                    "validation.password.mismatch",
                    "Passwords do not match"
            );
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("selectedUser", selectedUser);
            return "admin/change_user_password";
        }

        String username = userService.getUsernameByUserId(userId);

        userService.changeUserPassword(username, passwordChangeForm.getNewPassword());

        return "redirect:/admin/adminIndex";
    }

    @GetMapping("/admin/calculate_schedule_select")
    public String calculateScheduleMenu(Model model) {

        CalculationProfileForm calculationProfileForm = CalculationProfileForm.builder()
                .calculationMonth(YearMonth.now(ApplicationConstants.ZONE_ID).plusMonths(2))
                .shiftCountCap(5)
                .gapBetweenShifts(5)
                .forceFillShiftTypes(new ArrayList<>())
                .build();

        model.addAttribute(ModelAttributeName.CALCULATION_PROFILE_FORM, calculationProfileForm);
        model.addAttribute(ModelAttributeName.MONTH_OPTIONS, calculationProfileService.getAvailableCalculationMonths());
        model.addAttribute(ModelAttributeName.GAP_BETWEEN_SHIFTS, calculationProfileService.getGapBetweenShiftsOptions());
        model.addAttribute(ModelAttributeName.SHIFT_COUNT_MAX, calculationProfileService.getShiftCountCapOptions());
        model.addAttribute(ModelAttributeName.SHIFT_TYPES, calculationProfileService.getAvailableShiftTypes());

        return "admin/calculate_schedule_select";
    }

    @PostMapping("/admin/new_calculation")
    public String calculateSchedule(
            Model model,
            @ModelAttribute("calculationProfileForm") CalculationProfileForm calculationProfileForm
    ) {
        ScheduleCalendar bestCalendar =
                scheduleCalculationService.calculateSchedule(calculationProfileForm);

        ScheduleEditForm scheduleEditForm =
                scheduleMapper.toEditForm(bestCalendar, calculationProfileForm);

        ScheduleValidationResult scheduleValidationResult =
                scheduleValidationService.generateUserStats(bestCalendar);

        addScheduleTableAttributes(model, scheduleEditForm, scheduleValidationResult);

        return "admin/schedule_table";
    }

    @PostMapping("/admin/evaluate_edit")
    public String evaluateEdit(
            Model model,
            @ModelAttribute("scheduleEditForm") ScheduleEditForm scheduleEditForm,
            @RequestParam(defaultValue = "false") boolean saveSchedule
    ) {
        ScheduleValidationResult validationResult =
                scheduleValidationService.evaluateEdit(scheduleEditForm, saveSchedule);

        addScheduleTableAttributes(model, scheduleEditForm, validationResult);

        if (saveSchedule && !validationResult.isErrorsExist()) {
            return "redirect:/admin/show_saved_calendars";
        }

        return "admin/schedule_table";
    }

    private void addScheduleTableAttributes(
            Model model,
            ScheduleEditForm scheduleEditForm,
            ScheduleValidationResult validationResult
    ) {
        List<Integer> shiftTypes = IntStream.rangeClosed(1, shiftTypeService.getShiftTypes().getLast())
                .boxed()
                .toList();

        Set<String> usersWithNoRequest =
                scheduleCalculationService.returnUsersWithNoRequest();

        model.addAttribute("scheduleEditForm", scheduleEditForm);
        model.addAttribute("scheduleValidationResult", validationResult);

        model.addAttribute("shiftTypes", shiftTypes);
        model.addAttribute("users", userService.findAllUsersForSelectionByNameAsc());

        model.addAttribute("usersWithNoRequest", usersWithNoRequest);
        model.addAttribute("usersWithNoRequestString", String.join(", ", usersWithNoRequest));

        /*
         * Legacy aliases.
         */
        model.addAttribute("shiftCountCap", scheduleEditForm.getShiftCountCap());
        model.addAttribute("minimalGap", scheduleEditForm.getGapBetweenShifts());
        model.addAttribute("forceFillSelected",
                scheduleEditForm.isSortByDatesAmount()
                        || !scheduleEditForm.getForceFillShiftTypes().isEmpty());
        model.addAttribute("forceFillShiftTypes", scheduleEditForm.getForceFillShiftTypes());


    }


}
