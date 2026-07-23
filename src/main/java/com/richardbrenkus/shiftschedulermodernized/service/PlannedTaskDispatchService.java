package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.entity.ReminderEmailOutbox;
import com.richardbrenkus.shiftschedulermodernized.entity.SendReminderTask;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.ReminderEmailOutboxRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.SendReminderTaskRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlannedTaskDispatchService {

    private final SendReminderTaskRepository sendReminderTaskRepository;
    private final ReminderEmailOutboxRepository reminderEmailOutboxRepository;
    private final UserRepository userRepository;
    private final ActivityPublisher activityPublisher;

    @Transactional
    public void createReminderOutboxJobsIfDue(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }

        SendReminderTask task = sendReminderTaskRepository
                        .findById(SendReminderTask.SINGLETON_ID)
                        .orElseThrow(() -> new IllegalStateException("Required singleton row with ID " + SendReminderTask.SINGLETON_ID + " is missing from " + "send_reminder_task"));

        if (!isDue(task, now)) {
            return;
        }

        LocalDateTime scheduledOccurrence = task.getStartSendingTime();

        LocalDate finalSubmissionDay = task.getFinalRequestSubmissionDate().toLocalDate();

        List<User> recipients = userRepository.findUsersWithoutActiveShiftRequest();

        int createdJobCount = createRecipientOutboxJobs(task, scheduledOccurrence, finalSubmissionDay, recipients, now);

        advanceReminderTask(task);

        /*
         * This event is registered while the transaction is active, but
         * ActivityPublisher publishes it only after a successful commit.
         */
        activityPublisher.publishSuccess(
                ActivityType.REMINDER_EMAIL_JOB_CREATED,
                "SendReminderTask",
                String.valueOf(task.getId()),
                "Reminder email jobs created for scheduled "
                        + "execution "
                        + scheduledOccurrence
                        + "; jobs created: "
                        + createdJobCount
        );
    }

    private boolean isDue(SendReminderTask task, LocalDateTime now) {
        return task.isActive()
                && task.getStartSendingTime() != null
                && task.getFinalRequestSubmissionDate() != null
                && !task.getStartSendingTime().isAfter(now);
    }

    private int createRecipientOutboxJobs(SendReminderTask task, LocalDateTime scheduledOccurrence, LocalDate finalSubmissionDay, List<User> recipients, LocalDateTime now) {
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

            ReminderEmailOutbox outbox = ReminderEmailOutbox.pending(task.getId(), scheduledOccurrence, finalSubmissionDay, user.getId(), user.getEmail(), resolveDisplayName(user), now);

            try {
                reminderEmailOutboxRepository.save(outbox);
            }
            catch(DataIntegrityViolationException ignored) {
                log.debug("Concurrency violation while creating reminder outbox job for user {}: {}", user.getId(), ignored.getMessage());
            }

            createdJobCount++;
        }

        return createdJobCount;
    }

    private boolean isEligibleRecipient(User user) {
        return user != null && user.getId() != null && user.getEmail() != null && !user.getEmail().isBlank();
    }

    private String resolveDisplayName(User user) {
        if (user.getName() != null
                && !user.getName().isBlank()) {
            return user.getName();
        }

        if (user.getUsername() != null
                && !user.getUsername().isBlank()) {
            return user.getUsername();
        }

        return null;
    }

    private void advanceReminderTask(
            SendReminderTask task
    ) {
        if (task.getRepetitions() <= 0) {
            throw new IllegalStateException(
                    "Reminder repetitions must be "
                            + "greater than zero"
            );
        }

        task.setCounter(task.getCounter() + 1);

        if (task.getCounter()
                >= task.getRepetitions()) {
            task.setActive(false);
            return;
        }

        int frequencyInDays =
                task.getFrequencyInDays();

        if (frequencyInDays <= 0) {
            throw new IllegalStateException(
                    "Reminder frequency must be "
                            + "greater than zero"
            );
        }

        task.setStartSendingTime(
                task.getStartSendingTime()
                        .plusDays(frequencyInDays)
        );
    }
}