package com.richardbrenkus.shiftschedulermodernized.activity;

import com.richardbrenkus.shiftschedulermodernized.entity.ActivityLog;
import com.richardbrenkus.shiftschedulermodernized.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogWriter {

    private final ActivityLogRepository activityLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(ActivityEvent event) {
        activityLogRepository.save(ActivityLog.from(event));
    }
}