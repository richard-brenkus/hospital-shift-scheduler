package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.activity.ActivityPublisher;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ActivityType;
import com.richardbrenkus.hospitalshiftscheduler.entity.ReminderEmailOutbox;
import com.richardbrenkus.hospitalshiftscheduler.entity.SendReminderTask;
import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import com.richardbrenkus.hospitalshiftscheduler.repository.ReminderEmailOutboxRepository;
import com.richardbrenkus.hospitalshiftscheduler.repository.SendReminderTaskRepository;
import com.richardbrenkus.hospitalshiftscheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlannedTaskDispatchService {

    private final SendReminderTaskRepository sendReminderTaskRepository;
    private final ReminderEmailOutboxRepository reminderEmailOutboxRepository;
    private final UserRepository userRepository;
    private final ActivityPublisher activityPublisher;
    private final ZoneId applicationZoneId;

    @Transactional
    public void createReminderOutboxJobsIfDue(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }

        SendReminderTask task = sendReminderTaskRepository.findByIdForUpdate(SendReminderTask.SINGLETON_ID).orElseThrow(() -> new IllegalStateException("Required singleton row with ID " + SendReminderTask.SINGLETON_ID + " is missing from send_reminder_task"));

        if (!isDue(task, now)) {
            return;
        }

        Instant scheduledOccurrence = task.getStartSendingTime();

        LocalDate finalSubmissionDay = task.getFinalRequestSubmissionDate();

        List<User> recipients = userRepository.findByShiftRequestIsNullOrderByNameAsc();

        int createdJobCount = createRecipientOutboxJobs(task, scheduledOccurrence, finalSubmissionDay, recipients, now);

        advanceReminderTask(task);

        activityPublisher.publishSuccess(ActivityType.REMINDER_EMAIL_JOB_CREATED, "SendReminderTask", String.valueOf(task.getId()), "Reminder email jobs created for scheduled execution " + scheduledOccurrence + "; jobs created: " + createdJobCount);
    }

    private boolean isDue(SendReminderTask task, Instant now) {
        return task.isActive() && task.getStartSendingTime() != null && task.getFinalRequestSubmissionDate() != null && !task.getStartSendingTime().isAfter(now);
    }

    private int createRecipientOutboxJobs(SendReminderTask task, Instant scheduledOccurrence, LocalDate finalSubmissionDay, List<User> recipients, Instant now) {
        if (recipients == null || recipients.isEmpty()) {
            return 0;
        }

        int createdJobCount = 0;

        for (User user : recipients) {
            if (!isEligibleRecipient(user)) {
                continue;
            }

            boolean alreadyQueued = reminderEmailOutboxRepository.existsBySourceTaskIdAndScheduledExecutionTimeAndRecipientUserId(task.getId(), scheduledOccurrence, user.getId());

            if (alreadyQueued) {
                continue;
            }

            Instant finalSubmissionDeadline = finalSubmissionDay.atTime(LocalTime.MAX).atZone(applicationZoneId).toInstant();

            ReminderEmailOutbox outbox = ReminderEmailOutbox.pending(task.getId(), scheduledOccurrence, finalSubmissionDay, finalSubmissionDeadline, user.getId(), user.getEmail(), resolveDisplayName(user), now);

            reminderEmailOutboxRepository.save(outbox);
            createdJobCount++;
        }

        return createdJobCount;
    }

    private boolean isEligibleRecipient(User user) {
        return user != null && user.getId() != null && user.getEmail() != null && !user.getEmail().isBlank();
    }

    private String resolveDisplayName(User user) {

        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }

        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }

        return null;
    }

    private void advanceReminderTask(SendReminderTask task) {

        if (task.getRepetitions() <= 0) {
            throw new IllegalStateException("Reminder repetitions must be greater than zero");
        }

        task.setCounter(task.getCounter() + 1);

        if (task.getCounter() >= task.getRepetitions()) {
            task.setActive(false);
            return;
        }

        int frequencyInDays = task.getFrequencyInDays();

        if (frequencyInDays <= 0) {
            throw new IllegalStateException("Reminder frequency must be greater than zero");
        }

        Instant startSendingTime = task.getStartSendingTime().atZone(applicationZoneId).plusDays(frequencyInDays).toInstant();

        task.setStartSendingTime(startSendingTime);
    }
}