package com.richardbrenkus.hospitalshiftscheduler.controller;

import com.richardbrenkus.hospitalshiftscheduler.config.SelectionLists;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ModelAttributeName;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.CleanupTaskForm;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.SendReminderTaskForm;
import com.richardbrenkus.hospitalshiftscheduler.service.PlannedTasksService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
public class PlannedTasksController {

    private final PlannedTasksService plannedTasksService;
    private final Clock applicationClock;

    @GetMapping("/admin/planned_tasks")
    public String showScheduledEvents(Model model) {
        prefillPage(model);
        return "admin/planned_tasks";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/cleanup_task")
    public String postCleanupTask(Model model, @Valid @ModelAttribute("cleanupTaskForm") CleanupTaskForm cleanupTaskForm, BindingResult bindingResult) {
        Instant now = Instant.now(applicationClock);

        if (cleanupTaskForm.isCleanupTaskActive() && !plannedTasksService.isCleanupTimeInFuture(cleanupTaskForm, now)) {
            bindingResult.rejectValue("cleanupDay", "error.cleanupDay");
            bindingResult.rejectValue("cleanupHour", "error.cleanupHour");
            bindingResult.rejectValue("cleanupMinute", "error.cleanupMinute");
        }

        if (bindingResult.hasErrors()) {
            prefillPage(model, true, false);
            return "admin/planned_tasks";
        }

        plannedTasksService.saveCleanupTask(cleanupTaskForm, now);
        return "redirect:/admin/planned_tasks";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/send_reminder_task")
    public String postSendReminderTask(Model model, @Valid @ModelAttribute("sendReminderTaskForm") SendReminderTaskForm sendReminderTaskForm, BindingResult bindingResult) {
        Instant now = Instant.now(applicationClock);

        if (sendReminderTaskForm.isSendReminderTaskActive()) {
            validateReminderTask(sendReminderTaskForm, bindingResult, now);
        }

        if (bindingResult.hasErrors()) {
            prefillPage(model, false, true);
            return "admin/planned_tasks";
        }

        plannedTasksService.saveSendReminderTask(sendReminderTaskForm, now);
        return "redirect:/admin/planned_tasks";
    }

    private void validateReminderTask(SendReminderTaskForm form, BindingResult bindingResult, Instant now) {
        if (!plannedTasksService.isFirstReminderInFuture(form, now)) {
            bindingResult.rejectValue("startSendingRemindersDay", "error.reminderStartFuture");
            bindingResult.rejectValue("startSendingRemindersHour", "error.reminderStartFuture");
            bindingResult.rejectValue("startSendingRemindersMinute", "error.reminderStartFuture");
        }

        if (plannedTasksService.hasDayError(form)) {
            bindingResult.rejectValue("startSendingRemindersDay", "error.startSendingRemindersDay");
        }

        if (!plannedTasksService.isSendRemindersSetupValid(form, now)) {
            bindingResult.rejectValue("reminderSendingFrequencyInDays", "error.reminderSendingFrequencyInDays");
            bindingResult.rejectValue("reminderRepetitions", "error.reminderRepetitions");
            bindingResult.rejectValue("finalSubmissionDay", "error.finalSubmissionDay");
        }
    }

    private void prefillPage(Model model) {
        model.addAttribute(ModelAttributeName.CLEANUP_TASK_RECORD, plannedTasksService.getCleanupTaskRecord());
        model.addAttribute(ModelAttributeName.SEND_REMINDER_TASK_RECORD, plannedTasksService.getSendReminderTaskRecord());
        model.addAttribute(ModelAttributeName.CLEANUP_TASK_FORM, plannedTasksService.getCleanupTaskForm());
        model.addAttribute(ModelAttributeName.SEND_REMINDER_TASK_FORM, plannedTasksService.getSendReminderTaskForm());

        addSelectionLists(model);
    }

    private void prefillPage(Model model, boolean preserveCleanupForm, boolean preserveReminderForm) {
        model.addAttribute(ModelAttributeName.CLEANUP_TASK_RECORD, plannedTasksService.getCleanupTaskRecord());
        model.addAttribute(ModelAttributeName.SEND_REMINDER_TASK_RECORD, plannedTasksService.getSendReminderTaskRecord());

        if (!preserveCleanupForm) {
            model.addAttribute(ModelAttributeName.CLEANUP_TASK_FORM, plannedTasksService.getCleanupTaskForm());
        }

        if (!preserveReminderForm) {
            model.addAttribute(ModelAttributeName.SEND_REMINDER_TASK_FORM, plannedTasksService.getSendReminderTaskForm());
        }

        addSelectionLists(model);
    }

    private void addSelectionLists(Model model) {
        model.addAttribute(ModelAttributeName.DAYS_LIST, IntStream.rangeClosed(1, YearMonth.now(applicationClock).lengthOfMonth()).boxed().toList());
        model.addAttribute(ModelAttributeName.HOURS_LIST, IntStream.rangeClosed(0, 23).boxed().toList());
        model.addAttribute(ModelAttributeName.MINUTES_LIST, IntStream.rangeClosed(0, 59).boxed().toList());
        model.addAttribute(ModelAttributeName.REPETITIONS_LIST, SelectionLists.GENERIC_ONE_TO_TEN_LIST);
        model.addAttribute(ModelAttributeName.FREQUENCY_LIST, IntStream.rangeClosed(0, 10).boxed().toList());
        model.addAttribute(ModelAttributeName.FINAL_SUBMISSION_DAYS_LIST, IntStream.rangeClosed(1, YearMonth.now(applicationClock).lengthOfMonth()).boxed().toList());
    }
}