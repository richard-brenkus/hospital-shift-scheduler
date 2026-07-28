package com.richardbrenkus.shiftschedulermodernized.mapper;

import com.richardbrenkus.shiftschedulermodernized.dto.view.CleanupTaskRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.SendReminderTaskRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.CleanupTask;
import com.richardbrenkus.shiftschedulermodernized.entity.SendReminderTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PlannedTaskMapper {

    private final ZoneId applicationZoneId;

    public CleanupTaskRecord entityToCleanupTaskRecord(CleanupTask entity) {
        Objects.requireNonNull(entity, "entity must not be null");

        if(entity.isActive()) {
            Objects.requireNonNull(entity.getExecutionTime(), "executionTime must not be null");
        }

        return new CleanupTaskRecord(entity.isActive(), entity.getExecutionTime().atZone(applicationZoneId));
    }

    public SendReminderTaskRecord entityToSendReminderTaskRecord(SendReminderTask entity) {
        Objects.requireNonNull(entity,"entity must not be null");

        if(entity.isActive()) {
            Objects.requireNonNull(entity.getStartSendingTime(),"entity.startSendingTime must not be null");
            Objects.requireNonNull(entity.getFinalRequestSubmissionDate(),"entity.finalRequestSubmissionDate must not be null");

            return new SendReminderTaskRecord(
                    true,
                    entity.getRepetitions(),
                    entity.getFrequencyInDays(),
                    entity.getStartSendingTime().atZone(applicationZoneId),
                    entity.getFinalRequestSubmissionDate().getDayOfMonth());
        }

        return new SendReminderTaskRecord(
                false,
                entity.getRepetitions(),
                entity.getFrequencyInDays(),
                null,
                0
        );
    }
}