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
        model.addAttribute("hasDayError", false);

        return "admin/planned_tasks";
    }

    @PostMapping("/admin/cleanup_task")
    public String postCleanupTask(Model model, @ModelAttribute("cleanupTaskForm") CleanupTaskForm cleanupTaskForm) {

        prefillPage(model);

        plannedTasksService.saveCleanupTask(cleanupTaskForm);

        prefillPage(model);
        model.addAttribute("hasDayError", false);

        return "admin/planned_tasks";
    }

    @PostMapping("/admin/send_reminder_task")
    public String postSendReminderTask(Model model, @ModelAttribute("sendReminderTaskForm") SendReminderTaskForm sendReminderTaskForm, BindingResult bindingResult) {

        prefillPage(model);

        boolean hasDayError = plannedTasksService.hasDayError(sendReminderTaskForm);

        if (hasDayError) {

            bindingResult.rejectValue("startSendingRemindersDay", "error.startSendingRemindersDay", "must be before final submission day");
            model.addAttribute("hasDayError", true);

            return "/admin/planned_tasks";
        }

        plannedTasksService.saveSendReminderTask(sendReminderTaskForm);

        prefillPage(model);
        model.addAttribute("hasDayError", false);

        return "admin/planned_tasks";
    }


    private void prefillPage(Model model) {

        CleanupTaskForm cleanupTaskForm = plannedTasksService.getCleanupTaskForm();
        SendReminderTaskForm sendReminderTaskForm = plannedTasksService.getSendReminderTaskForm();

        model.addAttribute("cleanupTaskForm", cleanupTaskForm);
        model.addAttribute("sendReminderTaskForm", sendReminderTaskForm);
        model.addAttribute("daysList", IntStream.rangeClosed(1, 31).boxed().toList());
        model.addAttribute("hoursList", IntStream.rangeClosed(0, 23).boxed().toList());
        model.addAttribute("minutesList", IntStream.rangeClosed(0, 59).boxed().toList());
        model.addAttribute("repetitionsList", IntStream.rangeClosed(1, 10).boxed().toList());
        model.addAttribute("frequencyList", IntStream.rangeClosed(1, 31).boxed().toList());
        model.addAttribute("finalSubmissionDaysList", IntStream.rangeClosed(1, 31).boxed().toList());

    }


}
	

