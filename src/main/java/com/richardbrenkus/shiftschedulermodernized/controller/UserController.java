package com.richardbrenkus.shiftschedulermodernized.controller;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ModelAttributeName;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.LandingPageRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ScheduledTasksRecord;
import com.richardbrenkus.shiftschedulermodernized.service.LandingPageService;
import com.richardbrenkus.shiftschedulermodernized.service.ScheduledTasksService;
import com.richardbrenkus.shiftschedulermodernized.service.UserIndexPageService;
import com.richardbrenkus.shiftschedulermodernized.service.UserService;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

import static com.richardbrenkus.shiftschedulermodernized.config.SelectionLists.SHIFT_COUNT_LIST;
import static com.richardbrenkus.shiftschedulermodernized.config.SelectionLists.WEEKEND_COUNT_LIST;

@Controller
public class UserController {

    private final LandingPageService landingPageService;
    private final UserService userService;
    private final UserIndexPageService userIndexPageService;
    private final ScheduledTasksService scheduledTasksService;
    private final ShiftRequestService shiftRequestService;

    public UserController(LandingPageService landingPageService, UserService userService, UserIndexPageService userIndexPageService, ScheduledTasksService scheduledTasksService, ShiftRequestService shiftRequestService) {
        this.landingPageService = landingPageService;
        this.userService = userService;
        this.userIndexPageService = userIndexPageService;
        this.scheduledTasksService = scheduledTasksService;
        this.shiftRequestService = shiftRequestService;
    }

    @GetMapping("/home")
    public String index1(@CurrentSecurityContext(expression = "authentication?.name") String name) {
        if ("anonymousUser".equals(name))
            return "redirect:/login";
        if ("admin".equalsIgnoreCase(name))
            return "redirect:/admin/adminIndex";
        else return "redirect:/user/userIndex";
    }

    @GetMapping("/")
    public String index2(@CurrentSecurityContext(expression = "authentication?.name") String name) {
        if ("anonymousUser".equals(name))
            return "redirect:/login";
        if ("admin".equalsIgnoreCase(name))
            return "redirect:/admin/adminIndex";
        else return "redirect:/user/userIndex";
    }

    @GetMapping("/index")
    public String index3(@CurrentSecurityContext(expression = "authentication?.name") String name) {
        if ("anonymousUser".equals(name))
            return "redirect:/login";
        if ("admin".equalsIgnoreCase(name))
            return "redirect:/admin/adminIndex";
        else return "redirect:/user/userIndex";
    }

    @GetMapping("/admin/adminIndex")
    public String adminIndex(@CurrentSecurityContext(expression = "authentication?.name") String username, Model model) {

        model.addAttribute(ModelAttributeName.DISPLAY_NAME, userService.getDisplayNameByUserName(username));

        LandingPageRecord landingPageRecord = landingPageService.getLandingPageRecord();
        model.addAttribute(ModelAttributeName.USER_COUNT, landingPageRecord.userCount());
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
        model.addAttribute(ModelAttributeName.USERNAME_PASSED, username);
        model.addAttribute(ModelAttributeName.IS_ADMIN, false);
        model.addAttribute(ModelAttributeName.CONFLICTING_DATES, false);
        model.addAttribute(ModelAttributeName.WEEKEND_LIST, WEEKEND_COUNT_LIST);
        model.addAttribute(ModelAttributeName.SHIFT_LIST, SHIFT_COUNT_LIST);

        ShiftRequestForm shiftRequestForm = userService.getShiftRequestForm(username);
        model.addAttribute(ModelAttributeName.SHIFT_REQUEST_FORM, shiftRequestForm);

        Optional<SubmittedShiftRequestRecord> submittedShiftRequestRecord = userService.getSubmittedShiftRequestRecord(username);
        submittedShiftRequestRecord.ifPresentOrElse(
                record -> {
                    model.addAttribute(ModelAttributeName.HAS_SHIFT_REQUEST, true);
                    model.addAttribute(ModelAttributeName.SUBMITTED_SHIFT_REQUEST_RECORD, record);
                },
                () -> model.addAttribute(ModelAttributeName.HAS_SHIFT_REQUEST, false)
        );

        return "user/userIndex";
    }
}
