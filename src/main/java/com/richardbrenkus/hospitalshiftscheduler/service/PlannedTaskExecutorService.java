package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.activity.ActivityPublisher;
import com.richardbrenkus.hospitalshiftscheduler.activity.RequestMetadata;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ActivityType;
import com.richardbrenkus.hospitalshiftscheduler.entity.CleanupTask;
import com.richardbrenkus.hospitalshiftscheduler.entity.SendReminderTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlannedTaskExecutorService {

    private final PlannedTaskDispatchService plannedTaskDispatchService;
    private final CleanupTaskExecutionService cleanupTaskExecutionService;
    private final ActivityPublisher activityPublisher;
    private final Clock applicationClock;

    private static final String ACTIVITY_TARGET_CLEANUP = "CleanupTask";
    private static final String ACTIVITY_TARGET_REMINDER_EMAIL = "SendReminderTask";

    @Scheduled(fixedDelayString = "${planned-tasks.executor.fixed-delay-ms:60000}")
    public void executeDueTasks() {
        Instant now = Instant.now(applicationClock);

        executeCleanup(now);
        createReminderOutboxJobs(now);
    }

    private void executeCleanup(Instant now) {
        try {
            cleanupTaskExecutionService.executeCleanupTaskIfDue(now);

        } catch (RuntimeException exception) {
            log.error("Scheduled cleanup execution failed", exception);

            /*
             * Publish outside the rolled-back transaction.
             */
            activityPublisher.publishFailure(ActivityType.PLANNED_CLEANUP_FAILED, ACTIVITY_TARGET_CLEANUP, String.valueOf(CleanupTask.SINGLETON_ID), "Scheduled cleanup failed for " + now, "Cleanup task execution failed", RequestMetadata.system());
        }
    }

    private void createReminderOutboxJobs(Instant now) {
        try {
            plannedTaskDispatchService.createReminderOutboxJobsIfDue(now);

        } catch (RuntimeException exception) {
            log.error("Creation of reminder email outbox jobs failed", exception);

            /*
             * Publish outside the rolled-back transaction.
             */
            activityPublisher.publishFailure(ActivityType.REMINDER_EMAIL_JOB_CREATION_FAILED, ACTIVITY_TARGET_REMINDER_EMAIL, String.valueOf(SendReminderTask.SINGLETON_ID), "Reminder email job creation failed for UTC " + now, "Unable to create reminder email outbox jobs", RequestMetadata.system());
        }
    }
}