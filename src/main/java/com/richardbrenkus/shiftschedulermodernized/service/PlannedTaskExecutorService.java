package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.entity.CleanupTask;
import com.richardbrenkus.shiftschedulermodernized.entity.SendReminderTask;
import com.richardbrenkus.shiftschedulermodernized.repository.CleanupTaskRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.SendReminderTaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PlannedTaskExecutorService {

    private final CleanupTaskRepository cleanupTaskRepository;
    private final SendReminderTaskRepository sendReminderTaskRepository;
    private final ShiftRequestService shiftRequestService;
    private final SmtpEmailReminderService emailReminderService;

    @Scheduled(fixedDelay = 60_000)
    public void executeDueTasks() {
        executeCleanupTaskIfDue();
        executeReminderTaskIfDue();
    }

    private void executeCleanupTaskIfDue() {
        CleanupTask task = cleanupTaskRepository
                .findFirstByIsActiveTrueOrderByExecutionTimeAsc()
                .orElse(null);

        if (task == null || task.getExecutionTime() == null) {
            return;
        }

        if (task.getExecutionTime().isAfter(LocalDateTime.now())) {
            return;
        }

        shiftRequestService.deleteAllShiftRequests();

        task.setActive(false);
        cleanupTaskRepository.save(task);
    }

    private void executeReminderTaskIfDue() {
        SendReminderTask task = sendReminderTaskRepository
                .findFirstByIsActiveTrueOrderByStartSendingTimeAsc()
                .orElse(null);

        if (task == null || task.getStartSendingTime() == null) {
            return;
        }

        if (task.getStartSendingTime().isAfter(LocalDateTime.now())) {
            return;
        }

        emailReminderService.sendShiftRequestReminderEmails(task.getFinalSubmissionDay());

        task.setCounter(task.getCounter() + 1);

        if (task.getCounter() >= task.getRepetitions()) {
            task.setActive(false);
        } else {
            task.setStartSendingTime(
                    task.getStartSendingTime().plusDays(task.getFrequencyInDays())
            );
        }

        sendReminderTaskRepository.save(task);
    }
}
