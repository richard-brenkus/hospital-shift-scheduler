package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.activity.RequestMetadata;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlannedTaskExecutorService {

    private final PlannedTaskDispatchService plannedTaskDispatchService;
    private final CleanupTaskExecutionService cleanupTaskExecutionService;
    private final ActivityPublisher activityPublisher;
    private final Clock applicationClock;

    @Scheduled(
            fixedDelayString =
                    "${planned-tasks.executor.fixed-delay-ms:60000}"
    )
    public void executeDueTasks() {
        LocalDateTime now = LocalDateTime.now(applicationClock);

        executeCleanup(now);
        createReminderOutboxJobs(now);
    }

    private void executeCleanup(LocalDateTime now) {
        try {
            cleanupTaskExecutionService.executeCleanupTaskIfDue(now);

        } catch (RuntimeException exception) {
            log.error("Scheduled cleanup execution failed", exception);

            /*
             * Publish outside the rolled-back transaction.
             */
            activityPublisher.publishFailure(
                    ActivityType.PLANNED_CLEANUP_FAILED,
                    "CleanupTask",
                    null,
                    "Scheduled cleanup failed",
                    "Cleanup task execution failed",
                    RequestMetadata.system()
            );
        }
    }

    private void createReminderOutboxJobs(LocalDateTime now) {
        try {
            plannedTaskDispatchService.createReminderOutboxJobsIfDue(now);

        } catch (RuntimeException exception) {
            log.error(
                    "Creation of reminder email outbox jobs failed",
                    exception
            );

            /*
             * Publish outside the rolled-back transaction.
             */
            activityPublisher.publishFailure(
                    ActivityType.REMINDER_EMAIL_JOB_CREATION_FAILED,
                    "SendReminderTask",
                    null,
                    "Reminder email job creation failed",
                    "Unable to create reminder email outbox jobs",
                    RequestMetadata.system()
            );
        }
    }
}