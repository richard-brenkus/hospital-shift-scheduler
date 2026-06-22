package com.richardbrenkus.shiftschedulermodernized.controller;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ModelAttributeName;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ModelAttributeValue;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Profession;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.LandingPageRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ScheduledTasksRecord;
import com.richardbrenkus.shiftschedulermodernized.service.LandingPageService;
import com.richardbrenkus.shiftschedulermodernized.service.ScheduledTasksService;
import com.richardbrenkus.shiftschedulermodernized.service.ShiftTypeService;
import com.richardbrenkus.shiftschedulermodernized.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminController {

    private final LandingPageService landingPageService;
    private final UserService userService;
    private final ScheduledTasksService scheduledTasksService;
    private final ShiftTypeService shiftTypeService;

    public AdminController(LandingPageService landingPageService, UserService userService, ScheduledTasksService scheduledTasksService, ShiftTypeService shiftTypeService) {
        this.landingPageService = landingPageService;
        this.userService = userService;
        this.scheduledTasksService = scheduledTasksService;
        this.shiftTypeService = shiftTypeService;
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
            prepareRegisterUserModel(model);
            return "admin/register_user";
        }

        userService.createUser(userForm);

        model.addAttribute(ModelAttributeName.ACTION_TYPE, ModelAttributeValue.ACTION_TYPE_ADD);
        return "admin/user_update_success";
    }

    private void prepareRegisterUserModel(Model model) {
        model.addAttribute(ModelAttributeName.PROFESSIONS, Profession.values());
        model.addAttribute(ModelAttributeName.SHIFT_TYPES, shiftTypeService.getShiftTypes());
        model.addAttribute(ModelAttributeName.ACTION_TYPE, ModelAttributeValue.ACTION_TYPE_ADD);
        model.addAttribute(ModelAttributeName.HEADER_TYPE, ModelAttributeValue.HEADER_TYPE_ADMIN_ADD);
    }








}
