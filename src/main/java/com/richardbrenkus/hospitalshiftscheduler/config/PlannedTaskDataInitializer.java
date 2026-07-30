package com.richardbrenkus.hospitalshiftscheduler.config;

import com.richardbrenkus.hospitalshiftscheduler.entity.CleanupTask;
import com.richardbrenkus.hospitalshiftscheduler.entity.SendReminderTask;
import com.richardbrenkus.hospitalshiftscheduler.repository.CleanupTaskRepository;
import com.richardbrenkus.hospitalshiftscheduler.repository.SendReminderTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlannedTaskDataInitializer implements ApplicationRunner {

    private final CleanupTaskRepository cleanupTaskRepository;
    private final SendReminderTaskRepository sendReminderTaskRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initializeCleanupTask();
        initializeSendReminderTask();
    }

    private void initializeCleanupTask() {
        if (!cleanupTaskRepository.existsById(CleanupTask.SINGLETON_ID)) {
            CleanupTask cleanupTask = new CleanupTask();
            cleanupTask.setId(CleanupTask.SINGLETON_ID);
            cleanupTask.setActive(false);

            cleanupTaskRepository.save(cleanupTask);
        }
    }

    private void initializeSendReminderTask() {
        if (!sendReminderTaskRepository.existsById(SendReminderTask.SINGLETON_ID)) {
            SendReminderTask reminderTask = new SendReminderTask();
            reminderTask.setId(SendReminderTask.SINGLETON_ID);
            reminderTask.setActive(false);
            reminderTask.setFrequencyInDays(0);
            reminderTask.setRepetitions(0);
            reminderTask.setCounter(0);

            sendReminderTaskRepository.save(reminderTask);
        }
    }
}
