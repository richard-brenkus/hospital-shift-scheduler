package com.richardbrenkus.shiftschedulermodernized.controller;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ModelAttributeName;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ValidationConstants;
import com.richardbrenkus.shiftschedulermodernized.dto.form.PasswordChangeForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ShiftRequestValidationResult;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ShiftRequestViewRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ShiftRequestMapper;
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

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final LandingPageService landingPageService;
    private final UserService userService;
    private final PrepareModelService prepareModelService;
    private final ScheduledTasksService scheduledTasksService;
    private final ShiftRequestService shiftRequestService;
    private final ShiftRequestMapper shiftRequestMapper;

    @GetMapping({"/", "/home", "/index"})
    public String index(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ADMIN.asAuthority()));

        return isAdmin
                ? "redirect:/admin/adminIndex"
                : "redirect:/user/userIndex";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "403";
    }


    @GetMapping("/user/userIndex")
    public String userIndex(Authentication authentication, Model model) {

        String username = authentication.getName();

        model.addAttribute(ModelAttributeName.DISPLAY_NAME, userService.getDisplayNameByUserName(username));
        model.addAttribute(ModelAttributeName.CONFLICTING_DATES, false);

        ShiftRequestForm shiftRequestForm = shiftRequestService.getShiftRequestForm(username);

        prepareModelService.populateUserIndexModelForUser(model, shiftRequestForm, false, username);

        Optional<ShiftRequestViewRecord> submittedShiftRequestRecord = shiftRequestService.getShiftRequestViewRecord(username);
        submittedShiftRequestRecord.ifPresentOrElse(record -> {
                    model.addAttribute(ModelAttributeName.HAS_SHIFT_REQUEST, userService.hasShiftRequest(username));
                    model.addAttribute(ModelAttributeName.SUBMITTED_SHIFT_REQUEST_RECORD, record);
                    System.out.println();
                },
                () -> model.addAttribute(ModelAttributeName.HAS_SHIFT_REQUEST, userService.hasShiftRequest(username))
        );

        return "user/userIndex";
    }

    @PostMapping("/request_submitted")
    public String requestSubmitted(
            @Valid @ModelAttribute(ModelAttributeName.SHIFT_REQUEST_FORM) ShiftRequestForm shiftRequestForm,
            BindingResult bindingResult,
            Model model,
            Authentication authentication,
            @RequestParam(required = false) String usernamePassed) {

        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Role.ADMIN.asAuthority()));

        String loggedInUsername = authentication.getName();
        String targetUsername = isAdmin && usernamePassed != null ? usernamePassed : loggedInUsername;

        User user = userService.getUserByUsername(targetUsername);

        model.addAttribute(ModelAttributeName.DISPLAY_NAME, userService.getDisplayNameByUserName(targetUsername));
        prepareModelService.populateUserIndexModelForUser(model, shiftRequestForm, user.isAdmin(), usernamePassed);

        Optional<ShiftRequestViewRecord> submittedShiftRequestRecord = shiftRequestService.getShiftRequestViewRecord(targetUsername);
        submittedShiftRequestRecord.ifPresentOrElse(record -> {
                    model.addAttribute(ModelAttributeName.HAS_SHIFT_REQUEST, true);
                    model.addAttribute(ModelAttributeName.SUBMITTED_SHIFT_REQUEST_RECORD, record);
                },
                () -> model.addAttribute(ModelAttributeName.HAS_SHIFT_REQUEST, false)
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
                bindingResult.rejectValue(rejectedField, ValidationConstants.ERROR_PREFIX + rejectedField);
            }

            return "user/userIndex";
        }

        ShiftRequest savedShiftRequest = shiftRequestService.submitShiftRequest(user.getUsername(), shiftRequestForm);

        model.addAttribute(ModelAttributeName.SUBMITTED_SHIFT_REQUEST_RECORD, shiftRequestMapper.entityToViewRecord(user, savedShiftRequest));

        model.addAttribute(ModelAttributeName.IS_ADMIN, isAdmin);
        model.addAttribute(ModelAttributeName.USERNAME_PASSED, usernamePassed);
        model.addAttribute(ModelAttributeName.HAS_SHIFT_REQUEST, true);
        model.addAttribute(ModelAttributeName.DISPLAY_NAME, userService.getDisplayNameByUserName(targetUsername));

        return "user/shift_request_summary";
    }

    @GetMapping("/user/change_password")
    public String showChangePassword(Model model) {

        model.addAttribute(ModelAttributeName.NO_MATCH, false);
        model.addAttribute(ValidationConstants.ERROR_PREFIX + ModelAttributeName.TOO_SHORT, false);
        model.addAttribute(ModelAttributeName.PASSWORD_CHANGE_FORM, new PasswordChangeForm());

        return "user/change_password";
    }

    @PostMapping("/user/change_password")
    public String changeUserPassword(
            @Valid @ModelAttribute PasswordChangeForm form,
            BindingResult bindingResult,
            Model model,
            Authentication authentication) {

        if (bindingResult.hasErrors()) {
            return "user/change_password";
        }

        if (!form.passwordsMatch()) {
            model.addAttribute(ModelAttributeName.NO_MATCH, true);
            form.setConfirmedPassword("");
            return "user/change_password";
        }

        String username = authentication.getName();
        userService.changeUserPassword(username, form.getNewPassword());

        return "user/password_changed";
    }

    @PostMapping("user/shift_request_summary")
    public String showRequest(Model model, @RequestParam(name = ModelAttributeName.USERNAME_PASSED) String username, Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Role.ADMIN.asAuthority()));

        User currentUser = userService.getUserByUsername(username);
        model.addAttribute(ModelAttributeName.IS_ADMIN, isAdmin);

        if (!currentUser.hasShiftRequest()) {
            if (isAdmin)
                return "redirect:/admin/adminIndex";
            else return "redirect:/user/userIndex";
        }

        model.addAttribute(ModelAttributeName.DISPLAY_NAME, userService.getDisplayNameByUserName(username));
        model.addAttribute(ModelAttributeName.SUBMITTED_SHIFT_REQUEST_RECORD, shiftRequestService.getShiftRequestViewRecord(username).orElse(null));
        model.addAttribute(ModelAttributeName.HAS_SHIFT_REQUEST, currentUser.hasShiftRequest());

        return "user/shift_request_summary";
    }

    @PostMapping("/user/deactivate_request")
    public String deactivateRequest(@RequestParam(name = ModelAttributeName.USERNAME_PASSED) String username,
                                    Authentication authentication,
                                    Model model) {

        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Role.ADMIN.asAuthority()));

        shiftRequestService.deleteShiftRequest(username);

        String redirectUrl = isAdmin
                ? "/admin/adminIndex"
                : "/user/userIndex";

        model.addAttribute(ModelAttributeName.REDIRECT_URL, redirectUrl);

        return "user/shift_request_deleted";
    }


}
