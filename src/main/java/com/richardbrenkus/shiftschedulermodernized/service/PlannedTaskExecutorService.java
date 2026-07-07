package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.entity.CleanupTask;
import com.richardbrenkus.shiftschedulermodernized.entity.SendReminderTask;
import com.richardbrenkus.shiftschedulermodernized.repository.CleanupTaskRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.SendReminderTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PlannedTaskExecutorService {

    private final CleanupTaskRepository cleanupTaskRepository;
    private final SendReminderTaskRepository sendReminderTaskRepository;
    private final ShiftRequestService shiftRequestService;

    /*
     * Spring profiles:
     * dev  -> LoggingEmailReminderService
     * prod -> SmtpEmailReminderService
     */
    private final EmailReminderService emailReminderService;

    /*
     * Timezone-aware application clock.
     * Defined in TimeConfig from app.time-zone.
     */
    private final Clock applicationClock;

    @Scheduled(fixedDelayString = "${planned-tasks.executor.fixed-delay-ms:60000}")
    public void executeDueTasks() {
        LocalDateTime now = LocalDateTime.now(applicationClock);

        executeCleanupTaskIfDue(now);
        executeReminderTaskIfDue(now);
    }

    private void executeCleanupTaskIfDue(LocalDateTime now) {
        CleanupTask task = cleanupTaskRepository
                .findFirstByIsActiveTrueOrderByExecutionTimeAsc()
                .orElse(null);

        if (task == null || task.getExecutionTime() == null) {
            return;
        }

        if (task.getExecutionTime().isAfter(now)) {
            return;
        }

        shiftRequestService.deleteAllShiftRequests();

        task.setActive(false);
        cleanupTaskRepository.save(task);
    }

    private void executeReminderTaskIfDue(LocalDateTime now) {
        SendReminderTask task = sendReminderTaskRepository
                .findFirstByIsActiveTrueOrderByStartSendingTimeAsc()
                .orElse(null);

        if (task == null || task.getStartSendingTime() == null) {
            return;
        }

        if (task.getStartSendingTime().isAfter(now)) {
            return;
        }

        emailReminderService.sendShiftRequestReminderEmails(task.getFinalSubmissionDay());

        task.setCounter(task.getCounter() + 1);

        if (task.getCounter() >= task.getRepetitions()) {
            task.setActive(false);
        } else {
            task.setStartSendingTime(task.getStartSendingTime().plusDays(task.getFrequencyInDays()));
        }

        sendReminderTaskRepository.save(task);
    }
}
