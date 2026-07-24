package com.richardbrenkus.shiftschedulermodernized.activity;

import com.richardbrenkus.shiftschedulermodernized.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogWriter {

    private final ActivityLogRepository activityLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(ActivityEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(
                event.requestMetadata(),
                "event.requestMetadata must not be null"
        );

        int affectedRows = activityLogRepository.insertIfAbsent(
                event.eventId(),
                event.activityType().name(),
                event.actorUsername(),
                event.actorRole().name(),
                event.targetType(),
                event.targetId(),
                event.description(),
                event.successful(),
                event.failureReason(),
                event.requestMetadata().requestMethod(),
                event.requestMetadata().requestPath(),
                event.requestMetadata().clientIp(),
                LocalDateTime.ofInstant(
                        event.occurredAt(),
                        ZoneOffset.UTC
                )
        );

        if (affectedRows == 0) {
            log.debug(
                    "Activity event {} was already persisted; "
                            + "duplicate delivery ignored",
                    event.eventId()
            );
        }
    }
}