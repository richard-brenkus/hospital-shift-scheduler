package com.richardbrenkus.shiftschedulermodernized.controller;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ModelAttributeName;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ModelAttributeValue;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Profession;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.LandingPageRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ScheduledTasksRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserSummaryViewRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserUpdateValidationResult;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
public class AdminController {

    private final LandingPageService landingPageService;
    private final UserService userService;
    private final ScheduledTasksService scheduledTasksService;
    private final ShiftTypeService shiftTypeService;
    private final ShiftRequestService shiftRequestService;
    private final PrepareModelService prepareModelService;

    public AdminController(LandingPageService landingPageService, UserService userService, ScheduledTasksService scheduledTasksService, ShiftTypeService shiftTypeService, ShiftRequestService shiftRequestService, PrepareModelService prepareModelService) {
        this.landingPageService = landingPageService;
        this.userService = userService;
        this.scheduledTasksService = scheduledTasksService;
        this.shiftTypeService = shiftTypeService;
        this.shiftRequestService = shiftRequestService;
        this.prepareModelService = prepareModelService;
    }

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
        model.addAttribute(ModelAttributeName.USER_FORM, new UserForm());
        model.addAttribute(ModelAttributeName.PROFESSIONS, Profession.values());
        model.addAttribute(ModelAttributeName.SHIFT_TYPES, shiftTypeService.getShiftTypes());
        model.addAttribute(ModelAttributeName.ACTION_TYPE, ModelAttributeValue.ACTION_TYPE_ADD);
        model.addAttribute(ModelAttributeName.HEADER_TYPE, ModelAttributeValue.HEADER_TYPE_ADMIN_ADD);

        return "admin/register_user";
    }

    @PostMapping(path = "/admin/add")
    public String addNewUser(@Valid @ModelAttribute(ModelAttributeName.USER_FORM) UserForm userForm,
                             BindingResult bindingResult,
                             Model model) {

        if (userForm.getAllowedShiftTypes() == null || userForm.getAllowedShiftTypes().isEmpty()) {
            bindingResult.rejectValue(
                    "allowedShiftTypes",
                    "error.allowedShiftTypes",
                    "Please select at least one shift type!"
            );
        }

        if (userService.existsByUsernameIgnoreCase(userForm.getUsername())) {
            bindingResult.rejectValue(
                    "username",
                    "error.username",
                    "This user name has already been used!"
            );
        }

        if (userService.existsByEmailIgnoreCase(userForm.getEmail())) {
            bindingResult.rejectValue(
                    "email",
                    "error.email",
                    "This email address has already been used!"
            );
        }

        if (userService.existsByNameIgnoreCase(userForm.getName())) {
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

        userService.createUser(userForm);

        model.addAttribute(ModelAttributeName.ACTION_TYPE, ModelAttributeValue.ACTION_TYPE_ADD);
        return "admin/user_update_success";
    }

    @GetMapping("/admin/delete_user")
    public String showDeleteUserPage(Model model) {
        List<User> users = userService.getAllUsersWithoutAdmin();

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

        final UserForm updatedUser = new UserForm();
        List<User> usersList = userService.getAllUsersWithShiftRequest();

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

        final UserForm updatedUser = new UserForm();
        List<User> usersList = userService.getAllUsersAndAdmins();

        model.addAttribute(ModelAttributeName.USERS, usersList);
        model.addAttribute(ModelAttributeName.UPDATED_USER, updatedUser);

        return "admin/update_user";
    }

    @GetMapping(path = "/admin/update_shift_request")
    public String updateRequest(Model model) {

        final User updatedUser = new User();
        List<User> usersList = userService.getAllUsersWithShiftRequest();

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

    @PostMapping(path = "admin/update_user")
    public String getUserUpdateForm(@ModelAttribute("user") User user, @RequestParam long id, Model model) {

        final UserForm updatedUser = userService.getUserFormByUserId(id);

        model.addAttribute(ModelAttributeName.USER_FORM, updatedUser);
        model.addAttribute(ModelAttributeName.PROFESSIONS, Profession.values());
        model.addAttribute(ModelAttributeName.SHIFT_TYPES, shiftTypeService.getShiftTypes());
        model.addAttribute(ModelAttributeName.ACTION_TYPE, ModelAttributeValue.ACTION_TYPE_UPDATE);
        model.addAttribute(ModelAttributeName.HEADER_TYPE, ModelAttributeValue.HEADER_TYPE_ADMIN_UPDATE);

        return "admin/register_user";
    }

    @PostMapping(path = "/admin/update")
    public String postUserUpdateForm(
            @Valid @ModelAttribute(ModelAttributeName.USER_FORM) UserForm updatedUser,
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
            return "admin/register_user";
        }

        userService.updateUser(updatedUser);

        return "admin/user_update_success";
    }

    @GetMapping("/admin/all_users_list")
    public String showUsers(Model model) {

        List<UserSummaryViewRecord> usersList = userService.getAllUserSummaryViewRecords();
        model.addAttribute(ModelAttributeName.IS_ADMIN, true);
        model.addAttribute(ModelAttributeName.USERS, usersList);
        return "admin/all_users_list";
    }


}
