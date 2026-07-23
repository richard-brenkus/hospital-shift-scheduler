package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.entity.CleanupTask;
import com.richardbrenkus.shiftschedulermodernized.repository.CleanupTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CleanupTaskExecutionService {

    private final CleanupTaskRepository cleanupTaskRepository;
    private final ShiftRequestService shiftRequestService;
    private final ActivityPublisher activityPublisher;

    @Transactional
    public void executeCleanupTaskIfDue(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }

        CleanupTask task = cleanupTaskRepository
                .findByIdForUpdate(CleanupTask.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Required singleton row with ID " + CleanupTask.SINGLETON_ID + " is missing from cleanup_task"));

        if (!isDue(task, now)) {
            return;
        }

        shiftRequestService.deleteAllShiftRequests();
        task.setActive(false);

        /*
         * ActivityPublisher defers this event until after this transaction
         * commits. A listener or activity-storage failure therefore cannot
         * roll back cleanup.
         */
        activityPublisher.publishSuccess(
                ActivityType.PLANNED_CLEANUP_EXECUTED,
                "CleanupTask",
                String.valueOf(task.getId()),
                "Scheduled cleanup executed"
        );
    }

    private boolean isDue(CleanupTask task, LocalDateTime now) {
        return task.isActive() && task.getExecutionTime() != null && !task.getExecutionTime().isAfter(now);
    }
}