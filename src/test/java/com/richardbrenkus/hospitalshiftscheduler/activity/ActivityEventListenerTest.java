package com.richardbrenkus.hospitalshiftscheduler.activity;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.ActivityType;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityEventListenerTest {

    @Mock
    private ActivityLogWriter activityLogWriter;

    @InjectMocks
    private ActivityEventListener listener;

    @Test
    void handle_shouldDelegateEventToActivityLogWriter() {
        ActivityEvent event = anySuccessEvent();

        listener.handle(event);

        verify(activityLogWriter).persist(event);
    }

    @Test
    void handle_shouldSwallowExceptions_whenWriterFails() {
        ActivityEvent event = anySuccessEvent();
        doThrow(new RuntimeException("boom")).when(activityLogWriter).persist(event);

        // The listener runs after the business transaction has already committed.
        // A persistence failure must never propagate back to the committed flow.
        assertThatCode(() -> listener.handle(event)).doesNotThrowAnyException();
        verify(activityLogWriter).persist(event);
    }

    private ActivityEvent anySuccessEvent() {
        return ActivityEvent.success(Instant.parse("2026-08-05T08:00:00Z"), ActivityType.USER_CREATED, "alice", Role.ADMIN, "User", "1", "Created", new RequestMetadata("POST", "/admin/add", "127.0.0.1"));
    }
}
