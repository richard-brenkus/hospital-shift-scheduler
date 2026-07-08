package com.richardbrenkus.shiftschedulermodernized.controller;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleValidationResult;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ApplicationConstants;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ModelAttributeName;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Profession;
import com.richardbrenkus.shiftschedulermodernized.dto.form.*;
import com.richardbrenkus.shiftschedulermodernized.dto.view.*;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ScheduleMapper;
import com.richardbrenkus.shiftschedulermodernized.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final LandingPageService landingPageService;
    private final UserService userService;
    private final PlannedTasksService plannedTasksService;
    private final ShiftTypeService shiftTypeService;
    private final ShiftRequestService shiftRequestService;
    private final PrepareModelService prepareModelService;
    private final CalculationProfileService calculationProfileService;
    private final ScheduleCalculationService scheduleCalculationService;
    private final ScheduleValidationService scheduleValidationService;
    private final ScheduleMapper scheduleMapper;
    private final UserStatisticService userStatisticService;
    private final StoredScheduleService storedScheduleService;
    private final SpreadsheetService spreadsheetService;
    private final UserExcelExportService userExcelExportService;
    private final Clock applicationClock;


    @GetMapping("/admin/adminIndex")
    public String adminIndex(Authentication authentication, Model model) {

        String username = authentication.getName();

        model.addAttribute(ModelAttributeName.DISPLAY_NAME, userService.getDisplayNameByUserName(username));

        LandingPageRecord landingPageRecord = landingPageService.getLandingPageRecord();
        model.addAttribute(ModelAttributeName.USER_COUNT, landingPageRecord.userCountWithoutAdmin());
        model.addAttribute(ModelAttributeName.SHIFT_REQUEST_COUNT, landingPageRecord.shiftRequestCount());
        model.addAttribute(ModelAttributeName.PERCENTAGE, landingPageRecord.percentage());

        model.addAttribute("sendReminderTaskRecord", plannedTasksService.getSendReminderTaskRecord());
        model.addAttribute("cleanupTaskRecord", plannedTasksService.getCleanupTaskRecord());

        return "admin/adminIndex";
    }

    @GetMapping("/admin/register_user")
    public String showRegistrationForm(Model model) {
        model.addAttribute(ModelAttributeName.USER_REGISTER_FORM, new UserRegisterForm());
        model.addAttribute(ModelAttributeName.PROFESSIONS, Profession.values());
        model.addAttribute(ModelAttributeName.SHIFT_TYPES, shiftTypeService.getShiftTypes());

        return "admin/register_user";
    }

    @PostMapping(path = "/admin/add")
    public String addNewUser(@Valid @ModelAttribute(ModelAttributeName.USER_REGISTER_FORM) UserRegisterForm userRegisterForm, BindingResult bindingResult, Model model) {

        prepareModelService.prepareRegisterUserModel(model);

        if (userRegisterForm.getAllowedShiftTypes() == null || userRegisterForm.getAllowedShiftTypes().isEmpty()) {
            bindingResult.rejectValue(
                    "allowedShiftTypes",
                    "error.allowedShiftTypes"
            );
        }

        if (userService.existsByUsernameIgnoreCase(userRegisterForm.getUsername())) {
            bindingResult.rejectValue(
                    "username",
                    "error.username"
            );
        }

        if (userService.existsByEmailIgnoreCase(userRegisterForm.getEmail())) {
            bindingResult.rejectValue(
                    "email",
                    "error.email"
            );
        }

        if (userService.existsByNameIgnoreCase(userRegisterForm.getName())) {
            bindingResult.rejectValue(
                    "name",
                    "error.name"
            );
        }

        if (bindingResult.hasErrors()) {
/*
            prepareModelService.prepareRegisterUserModel(model);
*/
            return "admin/register_user";
        }

        userService.createUser(userRegisterForm);

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

        return "admin/update_user";
    }

    @PostMapping(path = "/admin/update_user")
    public String postUserUpdateForm(
            @Valid @ModelAttribute(ModelAttributeName.USER_UPDATE_FORM) UserUpdateForm updatedUser, BindingResult bindingResult, Model model) {

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
                    "validation.password.mismatch"
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
            HttpSession session,
            @ModelAttribute("calculationProfileForm") CalculationProfileForm calculationProfileForm
    ) {
        ScheduleMonth scheduleMonth = scheduleCalculationService.calculateSchedule(calculationProfileForm);

        List<Integer> shiftTypes = shiftTypeService.getShiftTypes();
        ScheduleEditForm scheduleEditForm = scheduleMapper.toEditForm(scheduleMonth, calculationProfileForm, shiftTypes);

        ScheduleValidationResult scheduleValidationResult = scheduleValidationService.initializeValidationAndUserStats(scheduleMonth);

        userStatisticService.storeFullStatisticsInSession(session, scheduleValidationResult, scheduleEditForm);

        addScheduleTableAttributes(model, scheduleEditForm, scheduleValidationResult);

        return "admin/schedule_table";
    }

    @PostMapping("/admin/evaluate_edit")
    public String evaluateEdit(Model model, HttpSession session, @ModelAttribute("scheduleEditForm") ScheduleEditForm scheduleEditForm) {
        ScheduleValidationResult validationResult = scheduleValidationService.validateSchedule(scheduleEditForm);

        userStatisticService.storeFullStatisticsInSession(session, validationResult, scheduleEditForm);

        addScheduleTableAttributes(model, scheduleEditForm, validationResult);

        return "admin/schedule_table";
    }

    @PostMapping("/admin/recalculate_schedule")
    public String recalculateSchedule(Model model, HttpSession session, @ModelAttribute("scheduleEditForm") ScheduleEditForm scheduleEditForm) {
        CalculationProfileForm calculationProfileForm = scheduleEditForm.toCalculationProfileForm();

        return calculateSchedule(model, session, calculationProfileForm);
    }

    @GetMapping("/admin/show_full_current_statistics")
    public String showFullCurrentStats(Model model, HttpSession session) {
        userStatisticService.addFullStatisticsToModel(model, session, shiftTypeService.getShiftTypes());

        return "admin/full_monthly_statistics";
    }

    @GetMapping("/admin/show_stored_full_statistics")
    public String showStoredFullStatistics(Model model, @RequestParam("selectedMonth") YearMonth selectedMonth) {
        Map<Integer, Set<UserStatViewRecord>> stats = userStatisticService.findViewRecordsByYearMonth(selectedMonth);

        model.addAttribute("fullUserStatsByShiftType", stats);
        model.addAttribute("shiftTypes", shiftTypeService.getShiftTypes());
        model.addAttribute("statsExist",
                stats.values().stream().anyMatch(set -> set != null && !set.isEmpty()));
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedMonth.getYear());
        model.addAttribute("monthInt", selectedMonth.getMonthValue());

        return "admin/full_monthly_statistics";
    }

    @PostMapping("/admin/save_schedule")
    public String saveSchedule(Model model, HttpSession session, @ModelAttribute("scheduleEditForm") ScheduleEditForm scheduleEditForm) {
        ScheduleValidationResult validationResult = scheduleValidationService.validateSchedule(scheduleEditForm);

        if (validationResult.isErrorsExist()) {
            userStatisticService.storeFullStatisticsInSession(session, validationResult, scheduleEditForm);

            addScheduleTableAttributes(model, scheduleEditForm, validationResult);

            return "admin/schedule_table";
        }

        ScheduleMonth scheduleMonth = scheduleMapper.toScheduleMonth(scheduleEditForm, scheduleEditForm.toCalculationProfileForm());

        storedScheduleService.saveSchedule(scheduleMonth);

        userStatisticService.replaceStatsForMonth(scheduleEditForm.getMonth(), validationResult.getFullUserStatsByShiftType());

        return "redirect:/admin/show_saved_schedules";
    }

    @PostMapping("/admin/save_schedule_override_validation")
    public String saveScheduleOverrideValidation(@ModelAttribute("scheduleEditForm") ScheduleEditForm scheduleEditForm) {
        ScheduleMonth scheduleMonth = scheduleMapper.toScheduleMonth(scheduleEditForm, scheduleEditForm.toCalculationProfileForm());

        storedScheduleService.saveSchedule(scheduleMonth);

        return "redirect:/admin/show_saved_schedules";
    }


    @GetMapping("/admin/show_saved_schedules")
    public String showSavedSchedules(Model model) {
        addSavedScheduleSelectionAttributes(model, SavedScheduleSelectionForm.builder().build(), true);

        return "admin/show_saved_schedules";
    }

    @PostMapping("/admin/show_saved_schedules")
    public String showSavedSchedulesPost(Model model, @ModelAttribute("savedScheduleSelectionForm") SavedScheduleSelectionForm form) {
        YearMonth selectedMonth = form.getSelectedMonth();

        if (selectedMonth == null || !storedScheduleService.existsByMonth(selectedMonth)) {
            addSavedScheduleSelectionAttributes(model, form, false);
            return "admin/show_saved_schedules";
        }

        SavedScheduleView savedScheduleView = storedScheduleService.loadSavedScheduleView(selectedMonth);

        model.addAttribute("savedSchedule", savedScheduleView);
        //model.addAttribute("calendar", savedScheduleView);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("year", selectedMonth.getYear());
        model.addAttribute("monthInt", selectedMonth.getMonthValue());
        model.addAttribute("monthDaysList", savedScheduleView.monthDaysList());
        model.addAttribute("weekendsAndHolidays", savedScheduleView.weekendsAndHolidays());
        model.addAttribute("shiftTypes", shiftTypeService.getShiftTypes());

        return "admin/schedule_table_saved";
    }

    @PostMapping("/admin/download_schedule_as_spreadsheet")
    public void exportSavedScheduleToExcel(HttpServletResponse response, @RequestParam("selectedMonth") YearMonth selectedMonth) throws IOException {

        String filename = spreadsheetService.createFileName(selectedMonth);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        spreadsheetService.writeSavedSchedule(response.getOutputStream(), selectedMonth);
    }

    @GetMapping("/admin/download_userlist_as_spreadsheet")
    public void exportUsersToExcel(HttpServletResponse response) throws IOException {
        String timestamp = LocalDateTime.now(applicationClock).format(ApplicationConstants.FILE_TIMESTAMP_FORMATTER);
        String filename = "users_" + timestamp + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        userExcelExportService.exportUsers(response.getOutputStream());
    }


    private void addSavedScheduleSelectionAttributes(Model model, SavedScheduleSelectionForm form, boolean scheduleExists) {
        model.addAttribute("savedScheduleSelectionForm", form);
        model.addAttribute("monthOptions", storedScheduleService.getSelectableMonthOptions());
        model.addAttribute("scheduleExists", scheduleExists);
    }

    private void addScheduleTableAttributes(Model model, ScheduleEditForm scheduleEditForm, ScheduleValidationResult scheduleValidationResult) {
        model.addAttribute("scheduleEditForm", scheduleEditForm);
        model.addAttribute("scheduleValidationResult", scheduleValidationResult);
        model.addAttribute("shiftTypes", shiftTypeService.getShiftTypes());
        model.addAttribute("users", userService.findAllUsersForSelectionByNameAsc());

        Set<String> usersWithNoRequest = userStatisticService.returnUsersWithNoRequest();

        model.addAttribute("usersWithNoRequest", usersWithNoRequest);
        model.addAttribute("usersWithNoRequestString", String.join(", ", usersWithNoRequest));
    }


}
