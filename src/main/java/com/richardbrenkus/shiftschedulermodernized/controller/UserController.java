package com.richardbrenkus.shiftschedulermodernized.controller;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ModelAttributeName;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.LandingPageRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ScheduledTasksRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ShiftRequestValidationResult;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ShiftRequestViewRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ShiftRequestMapper;
import com.richardbrenkus.shiftschedulermodernized.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class UserController {

    private final LandingPageService landingPageService;
    private final UserService userService;
    private final UserIndexPageService userIndexPageService;
    private final ScheduledTasksService scheduledTasksService;
    private final ShiftRequestService shiftRequestService;
    private final ShiftRequestMapper shiftRequestMapper;

    public UserController(LandingPageService landingPageService, UserService userService, UserIndexPageService userIndexPageService, ScheduledTasksService scheduledTasksService, ShiftRequestService shiftRequestService, ShiftRequestMapper shiftRequestMapper) {
        this.landingPageService = landingPageService;
        this.userService = userService;
        this.userIndexPageService = userIndexPageService;
        this.scheduledTasksService = scheduledTasksService;
        this.shiftRequestService = shiftRequestService;
        this.shiftRequestMapper = shiftRequestMapper;
    }

    @GetMapping({"/", "/home", "/index"})
    public String index(@CurrentSecurityContext(expression = "authentication?.name") String name) {
        if (name == null || "anonymousUser".equals(name)) {
            return "redirect:/login";
        }

        if ("admin".equalsIgnoreCase(name)) {
            return "redirect:/admin/adminIndex";
        }

        return "redirect:/user/userIndex";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "403";
    }

    @GetMapping("/admin/adminIndex")
    public String adminIndex(@CurrentSecurityContext(expression = "authentication?.name") String username, Model model) {

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

    @GetMapping("/user/userIndex")
    public String userIndex(@CurrentSecurityContext(expression = "authentication?.name") String username, Model model) {

        model.addAttribute(ModelAttributeName.DISPLAY_NAME, userService.getDisplayNameByUserName(username));
        model.addAttribute(ModelAttributeName.CONFLICTING_DATES, false);

        ShiftRequestForm shiftRequestForm = userService.getShiftRequestForm(username);

        userIndexPageService.populateUserIndexModel(model, username, shiftRequestForm, false, username);

        Optional<ShiftRequestViewRecord> submittedShiftRequestRecord = userService.getShiftRequestViewRecord(username);
        submittedShiftRequestRecord.ifPresentOrElse(record -> {
                    model.addAttribute(ModelAttributeName.IS_SHIFT_REQUEST_ACTIVE, true);
                    model.addAttribute(ModelAttributeName.SUBMITTED_SHIFT_REQUEST_RECORD, record);
                },
                () -> model.addAttribute(ModelAttributeName.IS_SHIFT_REQUEST_ACTIVE, false)
        );

        return "user/userIndex";
    }

    @PostMapping("/request_submitted")
    public String requestSubmitted(
            @Valid @ModelAttribute("shiftRequestForm") ShiftRequestForm shiftRequestForm,
            BindingResult bindingResult,
            Model model,
            @CurrentSecurityContext(expression = "authentication?.name") String username,
            @RequestParam(defaultValue = "false") boolean isAdmin,
            @RequestParam String usernamePassed) {

        String targetUsername = isAdmin ? usernamePassed : username;
        User user = userService.getUserByUsername(targetUsername);

        model.addAttribute(ModelAttributeName.DISPLAY_NAME, userService.getDisplayNameByUserName(targetUsername));
        userIndexPageService.populateUserIndexModel(model, username, shiftRequestForm, isAdmin, usernamePassed);

        Optional<ShiftRequestViewRecord> submittedShiftRequestRecord = userService.getShiftRequestViewRecord(username);
        submittedShiftRequestRecord.ifPresentOrElse(record -> {
                    model.addAttribute(ModelAttributeName.IS_SHIFT_REQUEST_ACTIVE, true);
                    model.addAttribute(ModelAttributeName.SUBMITTED_SHIFT_REQUEST_RECORD, record);
                },
                () -> model.addAttribute(ModelAttributeName.IS_SHIFT_REQUEST_ACTIVE, false)
        );

        if (!isAdmin) {
            shiftRequestService.applyDefaultUserPriorities(shiftRequestForm);
        }

        ShiftRequestValidationResult validationResult = shiftRequestService.validateShiftRequest(shiftRequestForm);

        if (!validationResult.isValid()) {
            if (validationResult.modelFlag() != null) {
                model.addAttribute(validationResult.modelFlag(), true);
            }

            for (String rejectedField : validationResult.rejectedFields()) {
                bindingResult.rejectValue(rejectedField, "error." + rejectedField);
            }

            return "user/userIndex";
        }

        ShiftRequest savedShiftRequest = shiftRequestService.submitShiftRequest(user, shiftRequestForm);

        model.addAttribute(ModelAttributeName.SUBMITTED_SHIFT_REQUEST_RECORD, shiftRequestMapper.entityToViewRecord(savedShiftRequest, user));

        model.addAttribute(ModelAttributeName.IS_ADMIN, isAdmin);
        model.addAttribute(ModelAttributeName.USERNAME_PASSED, usernamePassed);
        model.addAttribute(ModelAttributeName.IS_SHIFT_REQUEST_ACTIVE, true);
        model.addAttribute(ModelAttributeName.DISPLAY_NAME, userService.getDisplayNameByUserName(targetUsername));

        return "user/request_summary";
    }

    @GetMapping("/user/change_password")
    public String showChangePassword(Model model) {

        model.addAttribute("noMatch", false);
        model.addAttribute("tooShort", false);
        model.addAttribute("newPassword", "");
        model.addAttribute("confirmNewPassword", "");

        return "user/change_password";
    }

    @PostMapping("/user/change_password")
    public String changeUserPassword(Model model, @RequestParam(name = "newPassword") String newPassword, @RequestParam(name = "confirmNewPassword") String confirmNewPassword, @CurrentSecurityContext(expression = "authentication?.name") String username) {

        if (!(newPassword.contentEquals(confirmNewPassword))) {
            model.addAttribute("newPassword", newPassword);
            model.addAttribute("confirmNewPassword", confirmNewPassword);
            model.addAttribute("noMatch", true);
            model.addAttribute("tooShort", false);

            return "user/change_password";
        }

        if (newPassword.length() < 8) {
            model.addAttribute("newPassword", newPassword);
            model.addAttribute("confirmNewPassword", confirmNewPassword);
            model.addAttribute("noMatch", false);
            model.addAttribute("tooShort", true);

            return "user/change_password";
        }

        userService.changePassword(username, newPassword);

        return "redirect:/user/userIndex";
    }
}
