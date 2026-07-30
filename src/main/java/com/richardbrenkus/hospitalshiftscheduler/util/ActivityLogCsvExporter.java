package com.richardbrenkus.hospitalshiftscheduler.util;

import com.richardbrenkus.hospitalshiftscheduler.dto.export.ActivityLogExportRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ActivityLogCsvExporter {

    private static final String COLUMN_SEPARATOR = ",";
    private static final String LINE_SEPARATOR = "\r\n";
    private static final char UTF_8_BOM = '\uFEFF';
    private final ZoneId applicationZoneId;

    public void export(List<ActivityLogExportRecord> entries, OutputStream outputStream) throws IOException {

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

        writer.write(UTF_8_BOM);
        writeHeader(writer);

        for (ActivityLogExportRecord entry : entries) {
            writeRow(writer, entry);
        }

        writer.flush();
    }

    private void writeHeader(BufferedWriter writer) throws IOException {
        writeValues(
                writer,
                "ID",
                "Occurred at",
                "Event ID",
                "Activity type",
                "Actor username",
                "Actor role",
                "Target type",
                "Target ID",
                "Description",
                "Successful",
                "Failure reason",
                "Request method",
                "Request path",
                "Client IP"
        );
    }

    private void writeRow(BufferedWriter writer, ActivityLogExportRecord entry) throws IOException {

        writeValues(
                writer,
                value(entry.id()),
                CalendarDateIdUtils.CSV_DATE_TIME.format(entry.occurredAt().atZone(applicationZoneId)),
                value(entry.eventId()),
                entry.activityType(),
                entry.actorUsername(),
                entry.actorRole(),
                entry.targetType(),
                entry.targetId(),
                entry.description(),
                Boolean.toString(entry.successful()),
                entry.failureReason(),
                entry.requestMethod(),
                entry.requestPath(),
                entry.clientIp()
        );
    }

    private void writeValues(BufferedWriter writer,String... values) throws IOException {

        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                writer.write(COLUMN_SEPARATOR);
            }

            writer.write(escape(values[index]));
        }

        writer.write(LINE_SEPARATOR);
    }

    private String escape(String value) {
        String safeValue = value == null ? "" : value;

        if (!safeValue.isEmpty() && isFormulaPrefix(safeValue.charAt(0))) {
            safeValue = "'" + safeValue;
        }

        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private boolean isFormulaPrefix(char firstCharacter) {
        return firstCharacter == '='
                || firstCharacter == '+'
                || firstCharacter == '-'
                || firstCharacter == '@';
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}