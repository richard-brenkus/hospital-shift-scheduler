package com.richardbrenkus.shiftschedulermodernized.controller;

import com.richardbrenkus.shiftschedulermodernized.dto.form.CleanupTaskForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.SendReminderTaskForm;
import com.richardbrenkus.shiftschedulermodernized.service.PlannedTasksService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
public class PlannedTasksController {

    private final PlannedTasksService plannedTasksService;

    @GetMapping("/admin/planned_tasks")
    public String showScheduledEvents(Model model) {
        prefillPage(model);
        return "admin/planned_tasks";
    }

    @PostMapping("/admin/cleanup_task")
    public String postCleanupTask(
            Model model,
            @ModelAttribute("cleanupTaskForm") CleanupTaskForm cleanupTaskForm,
            BindingResult bindingResult
    ) {
        if (cleanupTaskForm.isCleanupTaskActive()
                && !plannedTasksService.isCleanupTimeInFuture(cleanupTaskForm)) {
            bindingResult.rejectValue("cleanupDay", "error.cleanupDay");
            bindingResult.rejectValue("cleanupHour", "error.cleanupHour");
            bindingResult.rejectValue("cleanupMinute", "error.cleanupMinute");
        }

        if (bindingResult.hasErrors()) {
            prefillPageKeepingSubmittedCleanupForm(model);
            return "admin/planned_tasks";
        }

        plannedTasksService.saveCleanupTask(cleanupTaskForm);
        return "redirect:/admin/planned_tasks";
    }

    @PostMapping("/admin/send_reminder_task")
    public String postSendReminderTask(
            Model model,
            @ModelAttribute("sendReminderTaskForm") SendReminderTaskForm sendReminderTaskForm,
            BindingResult bindingResult
    ) {
        if (sendReminderTaskForm.isSendReminderTaskActive()) {
            validateReminderTask(sendReminderTaskForm, bindingResult);
        }

        if (bindingResult.hasErrors()) {
            prefillPageKeepingSubmittedReminderForm(model);
            return "admin/planned_tasks";
        }

        plannedTasksService.saveSendReminderTask(sendReminderTaskForm);
        return "redirect:/admin/planned_tasks";
    }

    private void validateReminderTask(
            SendReminderTaskForm form,
            BindingResult bindingResult
    ) {
        if (!plannedTasksService.isFirstReminderInFuture(form)) {
            bindingResult.rejectValue(
                    "startSendingRemindersDay",
                    "error.reminderStartFuture",
                    "must be in the future"
            );
            bindingResult.rejectValue(
                    "startSendingRemindersHour",
                    "error.reminderStartFuture",
                    "must be in the future"
            );
            bindingResult.rejectValue(
                    "startSendingRemindersMinute",
                    "error.reminderStartFuture",
                    "must be in the future"
            );
        }

        if (plannedTasksService.hasDayError(form)) {
            bindingResult.rejectValue(
                    "startSendingRemindersDay",
                    "error.startSendingRemindersDay"
            );
        }

        if (!plannedTasksService.isSendRemindersSetupValid(form)) {
            bindingResult.rejectValue(
                    "reminderSendingFrequencyInDays",
                    "error.reminderSendingFrequencyInDays"
            );
            bindingResult.rejectValue(
                    "reminderRepetitions",
                    "error.reminderRepetitions"
            );
            bindingResult.rejectValue(
                    "finalSubmissionDay",
                    "error.finalSubmissionDay"
            );
        }
    }

    private void prefillPage(Model model) {
        model.addAttribute(
                "cleanupTaskRecord",
                plannedTasksService.getCleanupTaskRecord()
        );
        model.addAttribute(
                "sendReminderTaskRecord",
                plannedTasksService.getSendReminderTaskRecord()
        );
        model.addAttribute(
                "cleanupTaskForm",
                plannedTasksService.getCleanupTaskForm()
        );
        model.addAttribute(
                "sendReminderTaskForm",
                plannedTasksService.getSendReminderTaskForm()
        );

        addSelectionLists(model);
    }

    private void prefillPageKeepingSubmittedReminderForm(Model model) {
        model.addAttribute(
                "cleanupTaskRecord",
                plannedTasksService.getCleanupTaskRecord()
        );
        model.addAttribute(
                "sendReminderTaskRecord",
                plannedTasksService.getSendReminderTaskRecord()
        );
        model.addAttribute(
                "cleanupTaskForm",
                plannedTasksService.getCleanupTaskForm()
        );

        addSelectionLists(model);
    }

    private void prefillPageKeepingSubmittedCleanupForm(Model model) {
        model.addAttribute(
                "cleanupTaskRecord",
                plannedTasksService.getCleanupTaskRecord()
        );
        model.addAttribute(
                "sendReminderTaskRecord",
                plannedTasksService.getSendReminderTaskRecord()
        );
        model.addAttribute(
                "sendReminderTaskForm",
                plannedTasksService.getSendReminderTaskForm()
        );

        addSelectionLists(model);
    }

    private void addSelectionLists(Model model) {
        model.addAttribute(
                "daysList",
                IntStream.rangeClosed(1, 31).boxed().toList()
        );
        model.addAttribute(
                "hoursList",
                IntStream.rangeClosed(0, 23).boxed().toList()
        );
        model.addAttribute(
                "minutesList",
                IntStream.rangeClosed(0, 59).boxed().toList()
        );
        model.addAttribute(
                "repetitionsList",
                IntStream.rangeClosed(1, 10).boxed().toList()
        );
        model.addAttribute(
                "frequencyList",
                IntStream.rangeClosed(1, 31).boxed().toList()
        );
        model.addAttribute(
                "finalSubmissionDaysList",
                IntStream.rangeClosed(1, 31).boxed().toList()
        );
    }
}