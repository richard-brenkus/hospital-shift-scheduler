package com.richardbrenkus.shiftschedulermodernized.activity;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.repository.ActivityLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogWriterTest {

    private static final UUID EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant OCCURRED_AT = Instant.parse("2026-08-05T08:00:00Z");

    @Mock
    private ActivityLogRepository activityLogRepository;

    @InjectMocks
    private ActivityLogWriter writer;

    @Test
    void persist_shouldForwardAllFieldsToRepositoryInsert() {
        RequestMetadata metadata = new RequestMetadata("POST", "/admin/add", "1.2.3.4");
        ActivityEvent event = new ActivityEvent(
                EVENT_ID,
                OCCURRED_AT,
                "alice",
                Role.ADMIN,
                ActivityType.USER_CREATED,
                "User",
                "42",
                "Created user",
                true,
                null,
                metadata
        );

        writer.persist(event);

        verify(activityLogRepository).insertIfAbsent(
                eq(EVENT_ID),
                eq("USER_CREATED"),
                eq("alice"),
                eq("ADMIN"),
                eq("User"),
                eq("42"),
                eq("Created user"),
                eq(true),
                eq(null),
                eq("POST"),
                eq("/admin/add"),
                eq("1.2.3.4"),
                eq(OCCURRED_AT)
        );
    }

    @Test
    void persist_shouldSucceed_evenWhenInsertIfAbsentReportsDuplicate() {
        when(activityLogRepository.insertIfAbsent(
                any(UUID.class),
                anyString(),
                anyString(),
                anyString(),
                any(),
                any(),
                any(),
                anyBoolean(),
                any(),
                any(),
                any(),
                any(),
                any(Instant.class)
        )).thenReturn(0); // duplicate — no row inserted

        ActivityEvent event = successEvent();

        assertThatCode(() -> writer.persist(event)).doesNotThrowAnyException();
    }

    @Test
    void persist_shouldRejectNullEvent() {
        assertThatThrownBy(() -> writer.persist(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("event");
    }

    private ActivityEvent successEvent() {
        return new ActivityEvent(
                EVENT_ID,
                OCCURRED_AT,
                "alice",
                Role.ADMIN,
                ActivityType.USER_CREATED,
                "User",
                "42",
                "Created user",
                true,
                null,
                new RequestMetadata("POST", "/admin/add", "1.2.3.4")
        );
    }
}
