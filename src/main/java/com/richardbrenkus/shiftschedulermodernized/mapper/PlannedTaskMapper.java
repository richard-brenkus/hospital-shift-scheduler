package com.richardbrenkus.shiftschedulermodernized.mapper;

import com.richardbrenkus.shiftschedulermodernized.dto.view.CleanupTaskRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.SendReminderTaskRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.CleanupTask;
import com.richardbrenkus.shiftschedulermodernized.entity.SendReminderTask;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PlannedTaskMapper {

    public CleanupTaskRecord entityToCleanupTaskRecord(CleanupTask entity) {
        return new CleanupTaskRecord(
                entity.isActive(),
                entity.getExecutionTime()
        );
    }

    public SendReminderTaskRecord entityToSendReminderTaskRecord(SendReminderTask entity) {
        return new SendReminderTaskRecord(
                entity.isActive(),
                entity.getRepetitions(),
                entity.getFrequencyInDays(),
                entity.getStartSendingTime(),
                entity.getFinalRequestSubmissionDate().getDayOfMonth()
        );
    }
}
