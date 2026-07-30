package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.dto.export.ActivityLogExportRecord;
import com.richardbrenkus.hospitalshiftscheduler.entity.ActivityLog;
import com.richardbrenkus.hospitalshiftscheduler.repository.ActivityLogRepository;
import com.richardbrenkus.hospitalshiftscheduler.util.ActivityLogCsvExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogCsvExportService {

    public static final int EXPORT_ROW_LIMIT = 5_000;

    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogCsvExporter activityLogCsvExporter;
    private final ZoneId applicationZoneId;

    @Transactional(readOnly = true)
    public void exportMostRecentEntries(OutputStream outputStream) throws IOException {
        List<ActivityLogExportRecord> records = activityLogRepository
                .findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(0, EXPORT_ROW_LIMIT))
                .stream()
                .map(this::toExportRecord)
                .toList();

        activityLogCsvExporter.export(records, outputStream);
    }

    private ActivityLogExportRecord toExportRecord(ActivityLog activityLog) {
        return new ActivityLogExportRecord(
                activityLog.getId(),
                activityLog.getOccurredAt().atZone(applicationZoneId).toInstant(),
                activityLog.getEventId(),
                enumName(activityLog.getActivityType()),
                nullToEmpty(activityLog.getActorUsername()),
                enumName(activityLog.getActorRole()),
                nullToEmpty(activityLog.getTargetType()),
                nullToEmpty(activityLog.getTargetId()),
                nullToEmpty(activityLog.getDescription()),
                activityLog.isSuccessful(),
                nullToEmpty(activityLog.getFailureReason()),
                nullToEmpty(activityLog.getRequestMethod()),
                nullToEmpty(activityLog.getRequestPath()),
                nullToEmpty(activityLog.getClientIp())
        );
    }

    private String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}